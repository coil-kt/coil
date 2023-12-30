package coil3.fetch

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.network.CACHE_CONTROL
import coil3.network.CONTENT_TYPE
import coil3.network.CacheResponse
import coil3.network.CacheStrategy
import coil3.network.HttpException
import coil3.network.MIME_TYPE_TEXT_PLAIN
import coil3.network.abortQuietly
import coil3.network.appendAllIfNameAbsent
import coil3.network.assertNotOnMainThread
import coil3.network.closeQuietly
import coil3.network.writeTo
import coil3.request.Options
import coil3.request.httpHeaders
import coil3.request.httpMethod
import coil3.util.MimeTypeMap
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.prepareRequest
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.discardRemaining
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.utils.io.ByteReadChannel
import okio.Buffer
import okio.FileSystem
import okio.IOException

class NetworkFetcher(
    private val url: String,
    private val options: Options,
    private val httpClient: Lazy<HttpClient>,
    private val diskCache: Lazy<DiskCache?>,
    private val cacheStrategy: Lazy<CacheStrategy>,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        var snapshot = readFromDiskCache()
        try {
            // Fast path: fetch the image from the disk cache without performing a network request.
            var output: CacheStrategy.Output? = null
            if (snapshot != null) {
                var cacheResponse = snapshot.toCacheResponse()
                if (cacheResponse != null) {
                    val input = CacheStrategy.Input(cacheResponse, newRequest(), options)
                    output = cacheStrategy.value.compute(input)
                    cacheResponse = output.cacheResponse
                }
                if (cacheResponse != null) {
                    return SourceFetchResult(
                        source = snapshot.toImageSource(),
                        mimeType = getMimeType(url, cacheResponse.contentType),
                        dataSource = DataSource.DISK,
                    )
                }
            }

            // Slow path: fetch the image from the network.
            val networkRequest = output?.networkRequest ?: newRequest()
            var result = executeNetworkRequest(networkRequest) { response ->
                // Write the response to the disk cache then open a new snapshot.
                val responseBody = response.bodyAsChannel()
                snapshot = writeToDiskCache(snapshot, output?.cacheResponse, response, responseBody)
                if (snapshot != null) {
                    return@executeNetworkRequest SourceFetchResult(
                        source = snapshot!!.toImageSource(),
                        mimeType = getMimeType(url, snapshot!!.toCacheResponse()?.contentType),
                        dataSource = DataSource.NETWORK,
                    )
                }

                // If we failed to read a new snapshot then read the response body if it's not empty.
                if (responseBody.availableForRead > 0) {
                    return@executeNetworkRequest SourceFetchResult(
                        source = responseBody.toImageSource(),
                        mimeType = getMimeType(url, response.headers[CONTENT_TYPE]),
                        dataSource = DataSource.NETWORK,
                    )
                }

                return@executeNetworkRequest null
            }

            // Fallback: if the response body is empty, execute a new network request without the
            // cache headers.
            if (result == null) {
                result = executeNetworkRequest(newRequest()) { response ->
                    SourceFetchResult(
                        source = response.bodyAsChannel().toImageSource(),
                        mimeType = getMimeType(url, response.headers[CONTENT_TYPE]),
                        dataSource = DataSource.NETWORK,
                    )
                }
            }

            return result
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

    private suspend fun writeToDiskCache(
        snapshot: DiskCache.Snapshot?,
        cacheResponse: CacheResponse?,
        networkResponse: HttpResponse,
        networkResponseBody: ByteReadChannel,
    ): DiskCache.Snapshot? {
        // Short circuit if we're not allowed to cache this response.
        if (!options.diskCachePolicy.writeEnabled) {
            snapshot?.closeQuietly()
            return null
        }

        // Open a new editor.
        val editor = if (snapshot != null) {
            snapshot.closeAndOpenEditor()
        } else {
            diskCache.value?.openEditor(diskCacheKey)
        }

        // Return null if we're unable to write to this entry.
        if (editor == null) return null

        try {
            // Write the response to the disk cache.
            if (networkResponse.status == HttpStatusCode.NotModified && cacheResponse != null) {
                // Combine and write the updated cache headers and discard the body.
                fileSystem.write(editor.metadata) {
                    CacheResponse(
                        response = networkResponse,
                        headers = Headers.build {
                            appendAll(networkResponse.headers)
                            appendAllIfNameAbsent(cacheResponse.responseHeaders)
                        },
                    ).writeTo(this)
                }
                networkResponse.discardRemaining()
            } else {
                // Write the response's cache headers and body.
                fileSystem.write(editor.metadata) {
                    CacheResponse(networkResponse).writeTo(this)
                }
                networkResponseBody.writeTo(fileSystem, editor.data)
            }
            return editor.commitAndOpenSnapshot()
        } catch (e: Exception) {
            editor.abortQuietly()
            throw e
        }
    }

    private fun newRequest(): HttpRequestBuilder {
        val request = HttpRequestBuilder()
        request.method = options.httpMethod
        request.url.takeFrom(url)
        request.headers.appendAll(options.httpHeaders)

        val diskRead = options.diskCachePolicy.readEnabled
        val networkRead = options.networkCachePolicy.readEnabled
        when {
            !networkRead && diskRead -> {
                request.header(CACHE_CONTROL, "only-if-cached, max-stale=2147483647")
            }

            networkRead && !diskRead -> if (options.diskCachePolicy.writeEnabled) {
                request.header(CACHE_CONTROL, "no-cache")
            } else {
                request.header(CACHE_CONTROL, "no-cache, no-store")
            }

            !networkRead && !diskRead -> {
                // This causes the request to fail with a 504 Unsatisfiable Request.
                request.header(CACHE_CONTROL, "no-cache, only-if-cached")
            }
        }

        return request
    }

    private suspend fun <T> executeNetworkRequest(
        request: HttpRequestBuilder,
        block: suspend (HttpResponse) -> T,
    ): T {
        // Prevent executing requests on the main thread that could block due to a
        // networking operation.
        if (options.networkCachePolicy.readEnabled) {
            assertNotOnMainThread()
        }

        return httpClient.value.prepareRequest(request).execute { response ->
            if (!response.status.isSuccess() && response.status != HttpStatusCode.NotModified) {
                throw HttpException(response)
            }
            block(response)
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
        return ImageSource(
            file = data,
            fileSystem = fileSystem,
            diskCacheKey = diskCacheKey,
            closeable = this,
        )
    }

    private suspend fun ByteReadChannel.toImageSource(): ImageSource {
        return ImageSource(
            source = Buffer().apply { writeTo(this) },
            fileSystem = fileSystem,
        )
    }

    private val diskCacheKey: String
        get() = options.diskCacheKey ?: url

    private val fileSystem: FileSystem
        get() = diskCache.value?.fileSystem ?: options.fileSystem

    class Factory(
        private val httpClient: Lazy<HttpClient> = lazy { HttpClient() },
        private val cacheStrategy: Lazy<CacheStrategy> = lazy { CacheStrategy() },
    ) : Fetcher.Factory<Uri> {

        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? {
            if (!isApplicable(data)) return null
            return NetworkFetcher(
                url = data.toString(),
                options = options,
                httpClient = httpClient,
                diskCache = lazy { imageLoader.diskCache },
                cacheStrategy = cacheStrategy,
            )
        }

        private fun isApplicable(data: Uri): Boolean {
            return data.scheme == "http" || data.scheme == "https"
        }
    }
}
