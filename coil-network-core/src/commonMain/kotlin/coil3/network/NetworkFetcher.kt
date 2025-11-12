package coil3.network

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.Uri
import coil3.annotation.InternalCoilApi
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.network.internal.CACHE_CONTROL
import coil3.network.internal.CONTENT_TYPE
import coil3.network.internal.HTTP_RESPONSE_NOT_MODIFIED
import coil3.network.internal.MIME_TYPE_TEXT_PLAIN
import coil3.network.internal.abortQuietly
import coil3.network.internal.assertNotOnMainThread
import coil3.network.internal.closeQuietly
import coil3.network.internal.readBuffer
import coil3.network.internal.requireBody
import coil3.network.internal.singleParameterLazy
import coil3.request.Options
import coil3.util.MimeTypeMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Buffer
import okio.FileSystem
import okio.IOException

/**
 * A [Fetcher] that fetches and caches images from the network.
 */
class NetworkFetcher(
    private val url: String,
    private val options: Options,
    private val networkClient: Lazy<NetworkClient>,
    private val diskCache: Lazy<DiskCache?>,
    private val cacheStrategy: Lazy<CacheStrategy>,
    private val connectivityChecker: ConnectivityChecker,
    private val pendingRequests: HashMap<String, CompletableDeferred<SourceFetchResult>>,
    private val mutex: Mutex,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val requestKey = generateRequestKey(url, options)

        val newDeferred = CompletableDeferred<SourceFetchResult>()
        val deferred = mutex.withLock {
            pendingRequests[requestKey] ?: newDeferred.also {
                pendingRequests[requestKey] = it
            }
        }
        if (deferred !== newDeferred) {
            return deferred.await()
        }

        var snapshot = readFromDiskCache()
        try {
            // Fast path: fetch the image from the disk cache without performing a network request.
            var readResult: CacheStrategy.ReadResult? = null
            var cacheResponse: NetworkResponse? = null
            if (snapshot != null) {
                // Always return files with empty metadata as it's likely they've been written
                // to the disk cache manually.
                if (fileSystem.metadata(snapshot.metadata).size == 0L) {
                    val result = SourceFetchResult(
                        source = snapshot.toImageSource(),
                        mimeType = getMimeType(url, null),
                        dataSource = DataSource.DISK,
                    )
                    deferred.complete(result)
                    return result
                }

                // Return the image from the disk cache if the cache strategy agrees.
                cacheResponse = snapshot.toNetworkResponseOrNull()
                if (cacheResponse != null) {
                    throwIfFailureResponseCode(cacheResponse)

                    readResult = cacheStrategy.value.read(cacheResponse, newRequest(), options)
                    if (readResult.response != null) {
                        val result = SourceFetchResult(
                            source = snapshot.toImageSource(),
                            mimeType = getMimeType(url, readResult.response.headers[CONTENT_TYPE]),
                            dataSource = DataSource.DISK,
                        )
                        deferred.complete(result)
                        return result
                    }
                }
            }

            // Prevent executing requests on the main thread that could block due to a
            // networking operation.
            if (options.networkCachePolicy.readEnabled) {
                assertNotOnMainThread()
            }

            // Slow path: fetch the image from the network.
            val networkRequest = readResult?.request ?: newRequest()
            var fetchResult = networkClient.value.executeRequest(networkRequest) { networkResponse ->
                // Write the response to the disk cache then open a new snapshot.
                snapshot = writeToDiskCache(snapshot, cacheResponse, networkRequest, networkResponse)

                throwIfFailureResponseCode(networkResponse)

                if (snapshot != null) {
                    cacheResponse = snapshot!!.toNetworkResponseOrNull()
                    val result = SourceFetchResult(
                        source = snapshot!!.toImageSource(),
                        mimeType = getMimeType(url, cacheResponse?.headers?.get(CONTENT_TYPE)),
                        dataSource = DataSource.NETWORK,
                    )
                    deferred.complete(result)
                    return@executeRequest result
                }

                // If we failed to read a new snapshot then read the response body if it's not empty.
                val responseBody = networkResponse.requireBody().readBuffer()
                if (responseBody.size > 0) {
                    val result = SourceFetchResult(
                        source = responseBody.toImageSource(),
                        mimeType = getMimeType(url, networkResponse.headers[CONTENT_TYPE]),
                        dataSource = DataSource.NETWORK,
                    )
                    deferred.complete(result)
                    return@executeRequest result
                }

                return@executeRequest null
            }

            // Fallback: if the response body is empty, execute a new network request without the
            // cache headers.
            if (fetchResult == null) {
                fetchResult = networkClient.value.executeRequest(newRequest()) { response ->
                    val result = SourceFetchResult(
                        source = response.requireBody().toImageSource(),
                        mimeType = getMimeType(url, response.headers[CONTENT_TYPE]),
                        dataSource = DataSource.NETWORK,
                    )
                    deferred.complete(result)
                    result
                }
            }

            return fetchResult
        } catch (e: Exception) {
            snapshot?.closeQuietly()
            deferred.completeExceptionally(e)
            throw e
        } finally {
            mutex.withLock {
                if (pendingRequests[requestKey] === deferred) {
                    pendingRequests.remove(requestKey)
                }
            }
        }
    }

    private fun throwIfFailureResponseCode(networkResponse: NetworkResponse) {
        // Throw if the network response returns a non-200 (or 304) response.
        if (networkResponse.code !in 200 until 300 &&
            networkResponse.code != HTTP_RESPONSE_NOT_MODIFIED
        ) {
            throw HttpException(networkResponse)
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
        cacheResponse: NetworkResponse?,
        networkRequest: NetworkRequest,
        networkResponse: NetworkResponse,
    ): DiskCache.Snapshot? {
        // Short circuit if we're not allowed to cache this response.
        if (!options.diskCachePolicy.writeEnabled) {
            snapshot?.closeQuietly()
            return null
        }

        val writeResult = cacheStrategy.value.write(cacheResponse, networkRequest, networkResponse, options)
        val modifiedNetworkResponse = writeResult.response ?: return null

        // Open a new editor. Return null if we're unable to write to this entry.
        val editor = if (snapshot != null) {
            snapshot.closeAndOpenEditor()
        } else {
            diskCache.value?.openEditor(diskCacheKey)
        } ?: return null

        // Write the network request metadata and the network response body to disk.
        try {
            fileSystem.write(editor.metadata) {
                CacheNetworkResponse.writeTo(modifiedNetworkResponse, this)
            }
            modifiedNetworkResponse.body?.writeTo(fileSystem, editor.data)
            return editor.commitAndOpenSnapshot()
        } catch (e: Exception) {
            editor.abortQuietly()
            networkResponse.body?.closeQuietly()
            modifiedNetworkResponse.body?.closeQuietly()
            throw e
        }
    }

    private fun newRequest(): NetworkRequest {
        val headers = options.httpHeaders.newBuilder()
        val diskRead = options.diskCachePolicy.readEnabled
        val networkRead = options.networkCachePolicy.readEnabled && connectivityChecker.isOnline()
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
            extras = options.extras,
        )
    }

    private fun generateRequestKey(url: String, options: Options): String {
        return buildString {
            append(url)
            append('|')
            append(options.httpMethod)
            append('|')
            append(options.httpBody?.hashCode() ?: 0)
            append('|')
            val headers = options.httpHeaders.asMap()
                .flatMap { (name, values) -> values.map { value -> name to value } }
                .sortedBy { it.first }
                .joinToString(separator = ",") { "${it.first}:${it.second}" }
            append(headers)
            append('|')
            append(options.networkCachePolicy)
            append('|')
            append(options.diskCachePolicy)
            append('|')
            append(options.diskCacheKey)
        }
    }

    /**
     * Parse the response's `content-type` header.
     *
     * "text/plain" is often used as a default/fallback MIME type.
     * Attempt to guess a better MIME type from the file extension.
     */
    @InternalCoilApi
    fun getMimeType(url: String, contentType: String?): String? {
        if (contentType == null || contentType.startsWith(MIME_TYPE_TEXT_PLAIN)) {
            MimeTypeMap.getMimeTypeFromUrl(url)?.let { return it }
        }
        return contentType?.substringBefore(';')
    }

    private fun DiskCache.Snapshot.toNetworkResponseOrNull(): NetworkResponse? {
        try {
            return fileSystem.read(metadata) {
                CacheNetworkResponse.readFrom(this)
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
        val buffer = Buffer()
        writeTo(buffer)
        return buffer.toImageSource()
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
        cacheStrategy: () -> CacheStrategy = { CacheStrategy.DEFAULT },
        connectivityChecker: (PlatformContext) -> ConnectivityChecker = ::ConnectivityChecker,
    ) : Fetcher.Factory<Uri> {
        private val networkClientLazy = lazy(networkClient)
        private val cacheStrategyLazy = lazy(cacheStrategy)
        private val connectivityCheckerLazy = singleParameterLazy(connectivityChecker)
        private val pendingRequests = HashMap<String, CompletableDeferred<SourceFetchResult>>()
        private val mutex = Mutex()

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
                connectivityChecker = connectivityCheckerLazy.get(options.context),
                pendingRequests = pendingRequests,
                mutex = mutex,
            )
        }

        private fun isApplicable(data: Uri): Boolean {
            return data.scheme == "http" || data.scheme == "https"
        }
    }
}
