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
import coil.network.HttpException
import coil.request.Options
import coil.util.abortQuietly
import coil.util.await
import coil.util.closeQuietly
import coil.util.getMimeTypeFromUrl
import coil.util.isMainThread
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.IOException
import okio.buffer
import okio.sink
import okio.source
import java.net.HttpURLConnection.HTTP_NOT_MODIFIED

internal class HttpUriFetcher(
    private val url: String,
    private val options: Options,
    private val callFactory: Lazy<Call.Factory>,
    private val diskCache: Lazy<DiskCache?>,
    private val respectCacheHeaders: Boolean
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        var snapshot = readFromDiskCache()
        try {
            // Fast path: fetch the image from the disk cache without performing a network request.
            val cacheStrategy: CacheStrategy
            if (snapshot != null) {
                // Always return cached images with empty metadata as they were likely added manually.
                if (snapshot.metadata.length() == 0L) {
                    return SourceResult(
                        source = snapshot.toImageSource(),
                        mimeType = getMimeType(url, null),
                        dataSource = DataSource.DISK
                    )
                }

                // Return the candidate from the cache if it is eligible.
                if (respectCacheHeaders) {
                    cacheStrategy = CacheStrategy.Factory(newRequest(), snapshot.toCacheResponse()).compute()
                    if (cacheStrategy.networkRequest == null && cacheStrategy.cacheResponse != null) {
                        return SourceResult(
                            source = snapshot.toImageSource(),
                            mimeType = getMimeType(url, cacheStrategy.cacheResponse.contentType),
                            dataSource = DataSource.DISK
                        )
                    }
                } else {
                    // Skip checking the cache headers if the option is disabled.
                    return SourceResult(
                        source = snapshot.toImageSource(),
                        mimeType = getMimeType(url, snapshot.toCacheResponse()?.contentType),
                        dataSource = DataSource.DISK
                    )
                }
            } else {
                cacheStrategy = CacheStrategy.Factory(newRequest(), null).compute()
            }

            // Slow path: fetch the image from the network.
            val request = cacheStrategy.networkRequest ?: newRequest()
            val response = executeNetworkRequest(request)
            val responseBody = checkNotNull(response.body) { "response body == null" }
            try {
                // Read the response from the disk cache after writing it.
                snapshot = writeToDiskCache(snapshot, request, response, cacheStrategy.cacheResponse)
                if (snapshot != null) {
                    return SourceResult(
                        source = snapshot.toImageSource(),
                        mimeType = getMimeType(url, snapshot.toCacheResponse()?.contentType),
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
        return if (options.diskCachePolicy.readEnabled) diskCache.value?.get(diskCacheKey) else null
    }

    private fun writeToDiskCache(
        snapshot: DiskCache.Snapshot?,
        request: Request,
        response: Response,
        cacheResponse: CacheResponse?
    ): DiskCache.Snapshot? {
        // Short circuit if we're not allowed to cache this response.
        if (!isCacheable(request, response)) {
            snapshot?.closeQuietly()
            return null
        }

        val editor = if (snapshot != null) {
            snapshot.closeAndEdit()
        } else {
            diskCache.value?.edit(diskCacheKey)
        } ?: return null
        try {
            // Write the response to the disk cache.
            if (response.code == HTTP_NOT_MODIFIED && cacheResponse != null) {
                // Only update the metadata.
                val combinedResponse = response.newBuilder()
                    .headers(combineHeaders(CacheResponse(response).responseHeaders, response.headers))
                    .build()
                editor.metadata.sink().buffer().use { CacheResponse(combinedResponse).writeTo(it) }
                response.body!!.closeQuietly()
            } else {
                // Update the metadata and the image data.
                editor.metadata.sink().buffer().use { CacheResponse(response).writeTo(it) }
                response.body!!.source().use { editor.data.sink().use(it::readAll) }
            }
            return editor.commitAndGet()
        } catch (e: Exception) {
            editor.abortQuietly()
            throw e
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun newRequest(): Request {
        val request = Request.Builder()
            .url(url)
            .headers(options.headers)
            .apply { options.tags.forEach { tag(it.key as Class<Any>, it.value) } }

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
        val response = if (isMainThread()) {
            if (options.networkCachePolicy.readEnabled) {
                // Prevent executing requests on the main thread that could block due to a
                // networking operation.
                throw NetworkOnMainThreadException()
            } else {
                // Work around: https://github.com/Kotlin/kotlinx.coroutines/issues/2448
                callFactory.value.newCall(request).execute()
            }
        } else {
            // Suspend and enqueue the request on one of OkHttp's dispatcher threads.
            callFactory.value.newCall(request).await()
        }
        if (!response.isSuccessful && response.code != HTTP_NOT_MODIFIED) {
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

    private fun isCacheable(request: Request, response: Response): Boolean {
        return options.diskCachePolicy.writeEnabled &&
            (!respectCacheHeaders || CacheStrategy.isCacheable(request, response))
    }

    private fun DiskCache.Snapshot.toCacheResponse(): CacheResponse? {
        try {
            return metadata.source().buffer().use(::CacheResponse)
        } catch (_: IOException) {
            // If we can't parse the metadata, ignore this entry.
            return null
        }
    }

    private fun DiskCache.Snapshot.toImageSource(): ImageSource {
        return ImageSource(file = data, diskCacheKey = diskCacheKey, closeable = this)
    }

    private fun ResponseBody.toImageSource(): ImageSource {
        return ImageSource(source = source(), context = options.context)
    }

    private val diskCacheKey get() = options.diskCacheKey ?: url

    class Factory(
        private val callFactory: Lazy<Call.Factory>,
        private val diskCache: Lazy<DiskCache?>,
        private val respectCacheHeaders: Boolean
    ) : Fetcher.Factory<Uri> {

        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!isApplicable(data)) return null
            return HttpUriFetcher(data.toString(), options, callFactory, diskCache, respectCacheHeaders)
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
