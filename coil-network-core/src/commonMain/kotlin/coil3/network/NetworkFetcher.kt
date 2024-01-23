@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package coil3.network

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.network.internal.CACHE_CONTROL
import coil3.network.internal.CONTENT_TYPE
import coil3.network.internal.MIME_TYPE_TEXT_PLAIN
import coil3.network.internal.abortQuietly
import coil3.network.internal.assertNotOnMainThread
import coil3.network.internal.readBuffer
import coil3.request.Options
import coil3.util.MimeTypeMap
import coil3.util.closeQuietly
import okio.Buffer
import okio.FileSystem
import okio.IOException

class NetworkFetcher(
    private val url: String,
    private val options: Options,
    private val networkClient: Lazy<NetworkClient>,
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
                        mimeType = getMimeType(url, cacheResponse.responseHeaders[CONTENT_TYPE]),
                        dataSource = DataSource.DISK,
                    )
                }
            }

            // Slow path: fetch the image from the network.
            val networkRequest = output?.networkRequest ?: newRequest()
            var result = executeNetworkRequest(networkRequest) { response ->
                // Write the response to the disk cache then open a new snapshot.
                val responseBody = checkNotNull(response.body) { "body == null" }
                snapshot = writeToDiskCache(snapshot, output?.cacheResponse, response, responseBody)
                if (snapshot != null) {
                    val cacheResponse = snapshot!!.toCacheResponse()
                    return@executeNetworkRequest SourceFetchResult(
                        source = snapshot!!.toImageSource(),
                        mimeType = getMimeType(url, cacheResponse?.responseHeaders?.get(CONTENT_TYPE)),
                        dataSource = DataSource.NETWORK,
                    )
                }

                // If we failed to read a new snapshot then read the response body if it's not empty.
                val responseBodyBuffer = responseBody.readBuffer()
                if (responseBodyBuffer.size > 0) {
                    return@executeNetworkRequest SourceFetchResult(
                        source = responseBodyBuffer.toImageSource(),
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
                        source = checkNotNull(response.body) { "body == null" }.toImageSource(),
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
        networkResponse: NetworkResponse,
        networkResponseBody: NetworkResponseBody,
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
            if (networkResponse.code == 304 && cacheResponse != null) {
                // Combine and write the updated cache headers and discard the body.
                fileSystem.write(editor.metadata) {
                    val headers = networkResponse.headers.newBuilder()
                    for ((key, values) in cacheResponse.responseHeaders.asMap()) {
                        if (networkResponse.headers[key] == null) {
                            headers[key] = values
                        }
                    }
                    CacheResponse(networkResponse, headers.build()).writeTo(this)
                }
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
            networkResponseBody.close()
            throw e
        }
    }

    private fun newRequest(): NetworkRequest {
        val headers = options.httpHeaders.newBuilder()
        val diskRead = options.diskCachePolicy.readEnabled
        val networkRead = options.networkCachePolicy.readEnabled
        when {
            !networkRead && diskRead -> {
                headers[CACHE_CONTROL] = "only-if-cached, max-stale=2147483647"
            }

            networkRead && !diskRead -> if (options.diskCachePolicy.writeEnabled) {
                headers[CACHE_CONTROL] = "no-cache"
            } else {
                headers[CACHE_CONTROL] = "no-cache, no-store"
            }

            !networkRead && !diskRead -> {
                // This causes the request to fail with a 504 Unsatisfiable Request.
                headers[CACHE_CONTROL] = "no-cache, only-if-cached"
            }
        }

        return NetworkRequest(
            url = url,
            method = options.httpMethod,
            headers = headers.build(),
            body = options.httpBody,
        )
    }

    private suspend fun <T> executeNetworkRequest(
        request: NetworkRequest,
        block: suspend (NetworkResponse) -> T,
    ): T {
        // Prevent executing requests on the main thread that could block due to a
        // networking operation.
        if (options.networkCachePolicy.readEnabled) {
            assertNotOnMainThread()
        }

        return networkClient.value.executeRequest(request) { response ->
            if (response.code !in 200 until 300 && response.code != 304) {
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

    private suspend fun NetworkResponseBody.toImageSource(): ImageSource {
        return ImageSource(
            source = Buffer().apply { writeTo(this) },
            fileSystem = fileSystem,
        )
    }

    private fun Buffer.toImageSource(): ImageSource {
        return ImageSource(
            source = this,
            fileSystem = fileSystem,
        )
    }

    private val diskCacheKey: String
        get() = options.diskCacheKey ?: url

    private val fileSystem: FileSystem
        get() = diskCache.value?.fileSystem ?: options.fileSystem

    class Factory(
        networkClient: () -> NetworkClient,
        cacheStrategy: () -> CacheStrategy,
    ) : Fetcher.Factory<Uri> {
        private val networkClientLazy = lazy(networkClient)
        private val cacheStrategyLazy = lazy(cacheStrategy)

        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? {
            if (!isApplicable(data)) return null
            return NetworkFetcher(
                url = data.toString(),
                options = options,
                networkClient = networkClientLazy,
                diskCache = lazy { imageLoader.diskCache },
                cacheStrategy = cacheStrategyLazy,
            )
        }

        private fun isApplicable(data: Uri): Boolean {
            return data.scheme == "http" || data.scheme == "https"
        }
    }
}
