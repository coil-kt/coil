package coil.fetch

import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.network.CacheResponse
import coil.network.CacheStrategy
import coil.network.CacheStrategy.Companion.combineHeaders
import coil.network.Clock
import coil.network.HTTP_NOT_MODIFIED
import coil.network.HttpException
import coil.network.MIME_TYPE_TEXT_PLAIN
import coil.network.assertNotOnMainThread
import coil.network.closeQuietly
import coil.request.Options
import coil.util.MimeTypeMap
import dev.drewhamilton.poko.Poko
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.prepareRequest
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import okio.FileSystem
import okio.IOException

class NetworkFetcher(
    private val url: String,
    private val options: Options,
    private val httpClient: Lazy<HttpClient>,
    private val diskCache: Lazy<DiskCache?>,
    private val clock: Lazy<Clock>,
    private val respectCacheHeaders: Boolean,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        var snapshot = readFromDiskCache()
        try {
            // Fast path: fetch the image from the disk cache without performing a network request.
            val cacheStrategy: CacheStrategy
            if (snapshot != null) {
                // Always return cached images with empty metadata as they were likely added manually.
                if (fileSystem.metadata(snapshot.metadata).size == 0L) {
                    return SourceFetchResult(
                        source = snapshot.toImageSource(),
                        mimeType = getMimeType(url, null),
                        dataSource = DataSource.DISK,
                    )
                }

                // Return the candidate from the cache if it is eligible.
                if (respectCacheHeaders) {
                    cacheStrategy = CacheStrategy.Factory(
                        request = newRequest(),
                        cacheResponse = snapshot.toCacheResponse(),
                        clock = clock.value,
                    ).compute()
                    if (cacheStrategy.networkRequest == null && cacheStrategy.cacheResponse != null) {
                        return SourceFetchResult(
                            source = snapshot.toImageSource(),
                            mimeType = getMimeType(url, cacheStrategy.cacheResponse.contentType),
                            dataSource = DataSource.DISK,
                        )
                    }
                } else {
                    // Skip checking the cache headers if the option is disabled.
                    return SourceFetchResult(
                        source = snapshot.toImageSource(),
                        mimeType = getMimeType(url, snapshot.toCacheResponse()?.contentType),
                        dataSource = DataSource.DISK,
                    )
                }
            } else {
                cacheStrategy = CacheStrategy.Factory(
                    request = newRequest(),
                    cacheResponse = null,
                    clock = clock.value,
                ).compute()
            }

            // Slow path: fetch the image from the network.
            var response = executeNetworkRequest(cacheStrategy.networkRequest!!)
            var responseBody = response.requireBody()

            // Write the response to the disk cache then open a new snapshot.
            snapshot = writeToDiskCache(
                snapshot = snapshot,
                request = cacheStrategy.networkRequest,
                response = response,
                cacheResponse = cacheStrategy.cacheResponse,
            )
            if (snapshot != null) {
                return SourceFetchResult(
                    source = snapshot.toImageSource(),
                    mimeType = getMimeType(url, snapshot.toCacheResponse()?.contentType),
                    dataSource = DataSource.NETWORK,
                )
            }

            // If we failed to read a new snapshot then read the response body if it's not empty.
            if (responseBody.contentLength() > 0) {
                return SourceFetchResult(
                    source = responseBody.toImageSource(),
                    mimeType = getMimeType(url, responseBody.contentType()),
                    dataSource = response.toDataSource(),
                )
            } else {
                // If the response body is empty, execute a new network request without the
                // cache headers.
                response.closeQuietly()
                response = executeNetworkRequest(newRequest())
                responseBody = response.requireBody()

                return SourceFetchResult(
                    source = responseBody.toImageSource(),
                    mimeType = getMimeType(url, responseBody.contentType()),
                    dataSource = response.toDataSource(),
                )
            }
        } catch (e: Exception) {
            snapshot?.closeQuietly()
            throw e
        }
    }

    private fun readFromDiskCache(): DiskCache.Snapshot? {
        if (options.diskCachePolicy.readEnabled) {
            return diskCache.value?.openSnapshot(diskCacheKey)
        } else {
            return null
        }
    }

    private fun writeToDiskCache(
        snapshot: DiskCache.Snapshot?,
        request: HttpRequest,
        response: HttpResponse,
        cacheResponse: CacheResponse?,
    ): DiskCache.Snapshot? {
        // Short circuit if we're not allowed to cache this response.
        if (!isCacheable(request, response)) {
            snapshot?.closeQuietly()
            return null
        }

        // Open a new editor.
        val editor = if (snapshot != null) {
            snapshot.closeAndOpenEditor()
        } else {
            diskCache.value?.openEditor(diskCacheKey)
        }

        // Return `null` if we're unable to write to this entry.
        if (editor == null) return null

        try {
            // Write the response to the disk cache.
            if (response.status.value == HTTP_NOT_MODIFIED && cacheResponse != null) {
                // Only update the metadata.
                val combinedResponse = response.newBuilder()
                    .headers(combineHeaders(cacheResponse.responseHeaders, response.headers))
                    .build()
                fileSystem.write(editor.metadata) {
                    CacheResponse(combinedResponse).writeTo(this)
                }
            } else {
                // Update the metadata and the image data.
                fileSystem.write(editor.metadata) {
                    CacheResponse(response).writeTo(this)
                }
                fileSystem.write(editor.data) {
                    response.body!!.source().readAll(this)
                }
            }
            return editor.commitAndOpenSnapshot()
        } catch (e: Exception) {
            editor.abortQuietly()
            throw e
        } finally {
            response.closeQuietly()
        }
    }

    private fun newRequest(): HttpRequestBuilder {
        val request = HttpRequestBuilder()
        request.method = HttpMethod.Get
        request.url.takeFrom(Url(url))

        val diskRead = options.diskCachePolicy.readEnabled
        val networkRead = options.networkCachePolicy.readEnabled
        when {
            !networkRead && diskRead -> {
                request.header("Cache-Control", "only-if-cached, max-stale=2147483647")
            }

            networkRead && !diskRead -> if (options.diskCachePolicy.writeEnabled) {
                request.header("Cache-Control", "no-cache")
            } else {
                request.header("Cache-Control", "no-cache, no-store")
            }

            !networkRead && !diskRead -> {
                // This causes the request to fail with a 504 Unsatisfiable Request.
                request.header("Cache-Control", "no-cache, only-if-cached")
            }
        }

        return request
    }

    private suspend fun executeNetworkRequest(
        request: HttpRequestBuilder,
        block: suspend (HttpResponse) -> Unit,
    ) {
        // Prevent executing requests on the main thread that could block due to a
        // networking operation.
        if (options.networkCachePolicy.readEnabled) {
            assertNotOnMainThread()
        }

        httpClient.value.prepareRequest(request).execute { response ->
            if (!response.status.isSuccess() && response.status.value != HTTP_NOT_MODIFIED) {
                throw HttpException(response)
            }
        }
    }

    /**
     * Parse the response's `content-type` header.
     *
     * "text/plain" is often used as a default/fallback MIME type.
     * Attempt to guess a better MIME type from the file extension.
     */
    internal fun getMimeType(url: String, contentType: String?): String? {
        if (contentType == null || contentType.startsWith(MIME_TYPE_TEXT_PLAIN)) {
            MimeTypeMap.getMimeTypeFromUrl(url)?.let { return it }
        }
        return contentType?.substringBefore(';')
    }

    private fun isCacheable(request: HttpRequest, response: HttpResponse): Boolean {
        return options.diskCachePolicy.writeEnabled &&
            (!respectCacheHeaders || CacheStrategy.isCacheable(request, response))
    }

    private fun DiskCache.Snapshot.toCacheResponse(): CacheResponse? {
        try {
            return fileSystem.read(metadata) {
                CacheResponse(this)
            }
        } catch (_: IOException) {
            // If we can't parse the metadata, ignore this entry.
            return null
        }
    }

    private fun DiskCache.Snapshot.toImageSource(): ImageSource {
        return ImageSource(data, fileSystem, diskCacheKey, this)
    }

    private fun ResponseBody.toImageSource(): ImageSource {
        return ImageSource(source(), options.fileSystem)
    }

    private fun Response.toDataSource(): DataSource {
        return if (networkResponse != null) DataSource.NETWORK else DataSource.DISK
    }

    private val diskCacheKey: String
        get() = options.diskCacheKey ?: url

    private val fileSystem: FileSystem
        get() = diskCache.value!!.fileSystem

    @Poko
    class Factory(
        private val httpClient: Lazy<HttpClient> = lazy { HttpClient() },
        private val clock: Lazy<Clock> = lazy { Clock() },
        private val respectCacheHeaders: Boolean = false,
    ) : Fetcher.Factory<Uri> {

        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!isApplicable(data)) return null
            return NetworkFetcher(
                url = data.toString(),
                options = options,
                httpClient = httpClient,
                diskCache = lazy { imageLoader.diskCache },
                clock = clock,
                respectCacheHeaders = respectCacheHeaders,
            )
        }

        private fun isApplicable(data: Uri): Boolean {
            return data.scheme == "http" || data.scheme == "https"
        }
    }
}
