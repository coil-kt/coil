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

        // Write the response metadata and response body to disk.
        return WriteResult(networkResponse)
    }
}
