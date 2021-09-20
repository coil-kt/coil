package coil.fetch

import android.net.Uri
import android.os.NetworkOnMainThreadException
import android.webkit.MimeTypeMap
import androidx.annotation.VisibleForTesting
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.network.HttpException
import coil.request.Options
import coil.util.await
import coil.util.closeQuietly
import coil.util.dispatcher
import coil.util.getMimeTypeFromUrl
import kotlinx.coroutines.MainCoroutineDispatcher
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.BufferedSource
import okio.sink
import kotlin.coroutines.coroutineContext

internal class HttpUrlFetcher(
    private val url: String,
    private val options: Options,
    private val callFactory: Call.Factory,
    private val diskCache: DiskCache?
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        // Fast path: fetch the image from the disk cache.
        readFromDiskCache()?.let { return it }

        // Slow path: fetch the image from the network.
        val response = executeNetworkRequest()
        val body = checkNotNull(response.body) { "response body == null" }
        val source = body.source()
        try {
            val isSuccessful = writeToDiskCache(source)
            if (isSuccessful) {
                val snapshot = diskCache?.get(url)
                if (snapshot != null) {
                    return SourceResult(
                        source = ImageSource(
                            file = snapshot.data,
                            diskCacheKey = url,
                            closeable = snapshot
                        ),
                        mimeType = null,
                        dataSource = if (response.cacheResponse != null) {
                            DataSource.DISK
                        } else {
                            DataSource.NETWORK
                        }
                    )
                }
            }

            return SourceResult(
                source = ImageSource(body.source(), options.context),
                mimeType = getMimeType(url, body),
                dataSource = if (response.cacheResponse != null) {
                    DataSource.DISK
                } else {
                    DataSource.NETWORK
                }
            )
        } catch (throwable: Throwable) {
            // Only close the source if an exception occurs.
            source.closeQuietly()
            throw throwable
        }
    }

    private suspend inline fun executeNetworkRequest(): Response {
        val request = Request.Builder().url(url).headers(options.headers)
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

        val response = if (coroutineContext.dispatcher is MainCoroutineDispatcher) {
            if (networkRead) {
                // Prevent executing requests on the main thread that could block due to a
                // networking operation.
                throw NetworkOnMainThreadException()
            } else {
                // Work around https://github.com/Kotlin/kotlinx.coroutines/issues/2448 by
                // blocking the current context.
                callFactory.newCall(request.build()).execute()
            }
        } else {
            // Suspend and enqueue the request on one of OkHttp's dispatcher threads.
            callFactory.newCall(request.build()).await()
        }
        if (!response.isSuccessful) {
            response.body?.close()
            throw HttpException(response)
        }
        return response
    }

    private fun readFromDiskCache(): SourceResult? {
        if (!options.diskCachePolicy.readEnabled) return null
        val snapshot = diskCache?.get(url) ?: return null
        return SourceResult(
            source = ImageSource(
                file = snapshot.data,
                diskCacheKey = url,
                closeable = snapshot
            ),
            mimeType = null,
            dataSource = DataSource.DISK
        )
    }

    private fun writeToDiskCache(source: BufferedSource): Boolean {
        if (!options.diskCachePolicy.writeEnabled) return false
        val editor = diskCache?.edit(url)
        if (editor != null) {
            try {
                source.use { it.readAll(editor.data.sink()) }
                editor.commit()
                return true
            } catch (_: Exception) {
                editor.abort()
            }
        }
        return false
    }

    /**
     * Parse the response's `content-type` header.
     *
     * "text/plain" is often used as a default/fallback MIME type.
     * Attempt to guess a better MIME type from the file extension.
     */
    @VisibleForTesting
    internal fun getMimeType(url: String, body: ResponseBody): String? {
        val rawContentType = body.contentType()?.toString()
        if (rawContentType == null || rawContentType.startsWith(MIME_TYPE_TEXT_PLAIN)) {
            MimeTypeMap.getSingleton().getMimeTypeFromUrl(url)?.let { return it }
        }
        return rawContentType?.substringBefore(';')
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
