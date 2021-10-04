package coil.fetch

import android.net.Uri
import android.os.NetworkOnMainThreadException
import android.webkit.MimeTypeMap
import androidx.annotation.VisibleForTesting
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.network.CacheStrategy
import coil.network.CacheStrategy.Companion.combineHeaders
import coil.network.CacheStrategy.Companion.isCacheable
import coil.network.HttpException
import coil.request.Options
import coil.request.Parameters
import coil.util.abortQuietly
import coil.util.await
import coil.util.closeQuietly
import coil.util.dispatcher
import coil.util.getMimeTypeFromUrl
import kotlinx.coroutines.MainCoroutineDispatcher
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.BufferedSink
import okio.buffer
import okio.sink
import okio.source
import java.net.HttpURLConnection.HTTP_NOT_MODIFIED
import kotlin.coroutines.coroutineContext

internal class HttpUrlFetcher(
    private val url: String,
    private val options: Options,
    private val callFactory: Call.Factory,
    private val diskCache: DiskCache?
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        // Fast path: fetch the image from the disk cache without performing a network request.
        var request = newRequest()
        var snapshot = readFromDiskCache()
        try {
            val metadata = snapshot?.let(::CacheResponse)
            val cacheStrategy = CacheStrategy.Factory(request, metadata).compute()
            if (cacheStrategy.cacheResponse != null && cacheStrategy.networkRequest == null) {
                return SourceResult(
                    source = snapshot!!.toImageSource(),
                    mimeType = getMimeType(url, cacheStrategy.cacheResponse.contentType()),
                    dataSource = DataSource.DISK
                )
            }

            // Close the current snapshot so we can open an editor later.
            snapshot?.closeQuietly()
            // Update the network request in case we need to verify the image.
            cacheStrategy.networkRequest?.let { request = it }
        } catch (e: Exception) {
            // Close the snapshot if there's an unexpected error.
            snapshot?.closeQuietly()
            throw e
        }

        // Slow path: fetch the image from the network.
        val response = executeNetworkRequest(request)
        try {
            // Read the response from the disk cache after writing it.
            snapshot = writeToDiskCache(request, response)
            val metadata = snapshot?.let(::CacheResponse)
            if (metadata != null) {
                try {
                    return SourceResult(
                        source = snapshot!!.toImageSource(),
                        mimeType = getMimeType(url, metadata.contentType()),
                        dataSource = DataSource.NETWORK
                    )
                } catch (e: Exception) {
                    snapshot?.closeQuietly()
                    throw e
                }
            }

            // Read the response directly from the response body.
            return SourceResult(
                source = response.body!!.toImageSource(),
                mimeType = getMimeType(url, response.body!!.contentType()),
                dataSource = if (response.networkResponse != null) DataSource.NETWORK else DataSource.DISK
            )
        } catch (e: Exception) {
            response.body!!.closeQuietly()
            throw e
        }
    }

    private fun readFromDiskCache(): DiskCache.Snapshot? {
        if (!options.diskCachePolicy.readEnabled) return null
        return diskCache?.get(url)
    }

    private fun writeToDiskCache(request: Request, response: Response): DiskCache.Snapshot? {
        if (!options.diskCachePolicy.writeEnabled) return null
        if (!isCacheable(request, response)) return null

        val editor = diskCache?.edit(url) ?: return null
        try {
            if (response.code == HTTP_NOT_MODIFIED) {
                // Only update the metadata.
                val combinedResponse = response.newBuilder()
                    .headers(combineHeaders(CacheResponse(response).responseHeaders, response.headers))
                    .build()
                editor.metadata.sink().buffer().use { CacheResponse(combinedResponse).writeTo(it) }
            } else {
                // Update the metadata and the image data.
                editor.metadata.sink().buffer().use { CacheResponse(response).writeTo(it) }
                editor.data.sink().buffer().use { it.writeAll(response.body!!.source()) }
            }
            return if (options.diskCachePolicy.readEnabled) {
                editor.commitAndGet()
            } else {
                editor.commit().run { null }
            }
        } catch (e: Exception) {
            editor.abortQuietly()
            throw e
        }
    }

    private fun newRequest(): Request {
        val request = Request.Builder()
            .url(url)
            .headers(options.headers)
            // Support attaching custom data to the network request.
            .tag(Parameters::class.java, options.parameters)

        val diskRead = options.diskCachePolicy.readEnabled
        val networkRead = options.networkCachePolicy.readEnabled
        when {
            !networkRead && diskRead -> {
                request.cacheControl(CacheControl.FORCE_CACHE)
            }
            networkRead && !diskRead -> if (options.diskCachePolicy.writeEnabled) {
                request.cacheControl(CacheControl.FORCE_NETWORK)
            } else {
                request.cacheControl(CACHE_CONTROL_FORCE_NETWORK_NO_CACHE)
            }
            !networkRead && !diskRead -> {
                // This causes the request to fail with a 504 Unsatisfiable Request.
                request.cacheControl(CACHE_CONTROL_NO_NETWORK_NO_CACHE)
            }
        }

        return request.build()
    }

    private suspend fun executeNetworkRequest(request: Request): Response {
        val response = if (coroutineContext.dispatcher is MainCoroutineDispatcher) {
            if (options.networkCachePolicy.readEnabled) {
                // Prevent executing requests on the main thread that could block due to a
                // networking operation.
                throw NetworkOnMainThreadException()
            } else {
                // Work around: https://github.com/Kotlin/kotlinx.coroutines/issues/2448
                callFactory.newCall(request).execute()
            }
        } else {
            // Suspend and enqueue the request on one of OkHttp's dispatcher threads.
            callFactory.newCall(request).await()
        }
        if (!response.isSuccessful) {
            response.body?.closeQuietly()
            throw HttpException(response)
        }
        checkNotNull(response.body) { "response body == null" }
        return response
    }

    /**
     * Parse the response's `content-type` header.
     *
     * "text/plain" is often used as a default/fallback MIME type.
     * Attempt to guess a better MIME type from the file extension.
     */
    @VisibleForTesting
    internal fun getMimeType(url: String, contentType: MediaType?): String? {
        val rawContentType = contentType?.toString()
        if (rawContentType == null || rawContentType.startsWith(MIME_TYPE_TEXT_PLAIN)) {
            MimeTypeMap.getSingleton().getMimeTypeFromUrl(url)?.let { return it }
        }
        return rawContentType?.substringBefore(';')
    }

    private fun DiskCache.Snapshot.toImageSource(): ImageSource {
        return ImageSource(file = data, diskCacheKey = url, closeable = this)
    }

    private fun ResponseBody.toImageSource(): ImageSource {
        return ImageSource(source = source(), context = options.context)
    }

    /** Holds the response metadata for an image in the disk cache. */
    class CacheResponse {

        private var lazyCacheControl: CacheControl? = null

        val sentRequestAtMillis: Long
        val receivedResponseAtMillis: Long
        val isTls: Boolean
        val responseHeaders: Headers

        constructor(snapshot: DiskCache.Snapshot) {
            snapshot.metadata.source().buffer().use { source ->
                this.sentRequestAtMillis = source.readUtf8LineStrict().toLong()
                this.receivedResponseAtMillis = source.readUtf8LineStrict().toLong()
                this.isTls = source.readUtf8LineStrict().toInt() > 0
                val responseHeadersLineCount = source.readUtf8LineStrict().toInt()
                val responseHeaders = Headers.Builder()
                for (i in 0 until responseHeadersLineCount) {
                    responseHeaders.add(source.readUtf8LineStrict())
                }
                this.responseHeaders = responseHeaders.build()
            }
        }

        constructor(response: Response) {
            this.sentRequestAtMillis = response.sentRequestAtMillis
            this.receivedResponseAtMillis = response.receivedResponseAtMillis
            this.isTls = response.handshake != null
            this.responseHeaders = response.headers
        }

        fun writeTo(sink: BufferedSink) {
            sink.writeDecimalLong(sentRequestAtMillis).writeByte('\n'.code)
            sink.writeDecimalLong(receivedResponseAtMillis).writeByte('\n'.code)
            sink.writeDecimalLong(if (isTls) 1 else 0).writeByte('\n'.code)
            sink.writeDecimalLong(responseHeaders.size.toLong()).writeByte('\n'.code)
            for (i in 0 until responseHeaders.size) {
                sink.writeUtf8(responseHeaders.name(i))
                    .writeUtf8(": ")
                    .writeUtf8(responseHeaders.value(i))
                    .writeByte('\n'.code)
            }
        }

        fun cacheControl(): CacheControl {
            return lazyCacheControl ?: CacheControl.parse(responseHeaders).also { lazyCacheControl = it }
        }

        fun contentType(): MediaType? {
            return responseHeaders[CONTENT_TYPE_HEADER]?.toMediaTypeOrNull()
        }
    }

    class Factory(
        private val callFactory: Call.Factory,
        private val diskCache: DiskCache?
    ) : Fetcher.Factory<Uri> {

        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!isApplicable(data)) return null
            return HttpUrlFetcher(data.toString(), options, callFactory, diskCache)
        }

        private fun isApplicable(data: Uri): Boolean {
            return data.scheme == "http" || data.scheme == "https"
        }
    }

    companion object {
        private const val MIME_TYPE_TEXT_PLAIN = "text/plain"
        private const val CONTENT_TYPE_HEADER = "Content-Type"
        private val CACHE_CONTROL_FORCE_NETWORK_NO_CACHE =
            CacheControl.Builder().noCache().noStore().build()
        private val CACHE_CONTROL_NO_NETWORK_NO_CACHE =
            CacheControl.Builder().noCache().onlyIfCached().build()
    }
}
