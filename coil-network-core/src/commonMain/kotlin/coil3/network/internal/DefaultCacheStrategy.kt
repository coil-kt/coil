package coil3.network.internal

import coil3.network.CacheStrategy
import coil3.network.CacheStrategy.ReadResult
import coil3.network.CacheStrategy.WriteResult
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.request.Options

internal class DefaultCacheStrategy : CacheStrategy {

    override suspend fun read(
        cacheResponse: NetworkResponse,
        networkRequest: NetworkRequest,
        options: Options,
    ): ReadResult {
        // Always return the disk cache response.
        return ReadResult(cacheResponse)
    }

    override suspend fun write(
        cacheResponse: NetworkResponse?,
        networkRequest: NetworkRequest,
        networkResponse: NetworkResponse,
        options: Options,
    ): WriteResult {
        // Combine the disk response and network response headers and update the metadata.
        // Don't update the response body on disk.
        if (networkResponse.code == HTTP_RESPONSE_NOT_MODIFIED && cacheResponse != null) {
            val combinedHeaders = cacheResponse.headers + networkResponse.headers
            return WriteResult(networkResponse.copy(headers = combinedHeaders, body = null))
        }

        // Write the response metadata and body to disk.
        // Cache all 2xx responses for backwards compatibility.
        if (networkResponse.code in 200 until 300 ||
            networkResponse.code in CACHEABLE_STATUS_CODES
        ) {
            return WriteResult(networkResponse)
        }

        // Disable writing to disk for non-cacheable HTTP status codes.
        return WriteResult.DISABLED
    }

    private companion object {
        // List of non-200 HTTP codes that are cacheable by default according to RFC semantics.
        private val CACHEABLE_STATUS_CODES = setOf(300, 301, 404, 405, 410, 414, 501)
    }
}
