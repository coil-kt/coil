package coil.fetch

import android.net.Uri
import android.os.NetworkOnMainThreadException
import android.webkit.MimeTypeMap
import androidx.annotation.VisibleForTesting
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.network.CacheResponse
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
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.buffer
import okio.sink
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
        var snapshot = readFromDiskCache()
        try {
            val cacheRequest = newRequest()
            val cacheStrategy = CacheStrategy.Factory(cacheRequest, CacheResponse.from(snapshot)).compute()
            if (cacheStrategy.cacheResponse != null && cacheStrategy.networkRequest == null) {
                return SourceResult(
                    source = snapshot!!.toImageSource(),
                    mimeType = getMimeType(url, cacheStrategy.cacheResponse.contentType()),
                    dataSource = DataSource.DISK
                )
            }

            // Slow path: fetch the image from the network.
            val networkRequest = cacheStrategy.networkRequest ?: cacheRequest
            val response = executeNetworkRequest(networkRequest)
            val responseBody = checkNotNull(response.body) { "response body == null" }
            try {
                // Read the response from the disk cache after writing it.
                val allowNotModified = cacheStrategy.networkRequest != null &&
                    cacheStrategy.cacheResponse != null
                snapshot = writeToDiskCache(snapshot, networkRequest, response, allowNotModified)
                val cacheResponse = CacheResponse.from(snapshot)
                if (cacheResponse != null) {
                    return SourceResult(
                        source = snapshot!!.toImageSource(),
                        mimeType = getMimeType(url, cacheResponse.contentType()),
                        dataSource = DataSource.NETWORK
                    )
                }

                // If we can't read it from the cache or write it to the cache, read the response
                // directly from the response body.
                return SourceResult(
                    source = responseBody.toImageSource(),
                    mimeType = getMimeType(url, responseBody.contentType()),
                    dataSource = if (response.networkResponse != null) DataSource.NETWORK else DataSource.DISK
                )
            } catch (e: Exception) {
                responseBody.closeQuietly()
                throw e
            }
        } catch (e: Exception) {
            snapshot?.closeQuietly()
            throw e
        }
    }

    private fun readFromDiskCache(): DiskCache.Snapshot? {
        if (!options.diskCachePolicy.readEnabled) return null
        return diskCache?.get(url)
    }

    private fun writeToDiskCache(
        snapshot: DiskCache.Snapshot?,
        request: Request,
        response: Response,
        allowNotModified: Boolean
    ): DiskCache.Snapshot? {
        if (!options.diskCachePolicy.writeEnabled) return null
        if (!isCacheable(request, response)) return null

        val editor = if (snapshot != null) {
            snapshot.closeAndEdit()
        } else {
            diskCache?.edit(url)
        } ?: return null
        try {
            if (allowNotModified && response.code == HTTP_NOT_MODIFIED) {
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
        private val CACHE_CONTROL_FORCE_NETWORK_NO_CACHE =
            CacheControl.Builder().noCache().noStore().build()
        private val CACHE_CONTROL_NO_NETWORK_NO_CACHE =
            CacheControl.Builder().noCache().onlyIfCached().build()
    }
}
