package coil3.network

import coil3.annotation.ExperimentalCoilApi
import coil3.network.internal.DefaultCacheStrategy
import coil3.request.Options
import kotlin.jvm.JvmField

/**
 * Determines whether to use a cached response from the disk cache and/or perform a new network request.
 */
@ExperimentalCoilApi
interface CacheStrategy {

    suspend fun read(
        cacheResponse: NetworkResponse,
        networkRequest: NetworkRequest,
        options: Options,
    ): ReadResult

    suspend fun write(
        cacheResponse: NetworkResponse?,
        networkRequest: NetworkRequest,
        networkResponse: NetworkResponse,
        options: Options,
    ): WriteResult

    class ReadResult {
        val cacheResponse: NetworkResponse?
        val networkRequest: NetworkRequest?

        /**
         * Create a result that will use [cacheResponse] as the image source.
         */
        constructor(cacheResponse: NetworkResponse) {
            this.cacheResponse = cacheResponse
            this.networkRequest = null
        }

        /**
         * Create a result that will execute [networkRequest] and use the response body as the
         * image source.
         */
        constructor(networkRequest: NetworkRequest) {
            this.cacheResponse = null
            this.networkRequest = networkRequest
        }
    }

    class WriteResult {
        val networkResponse: NetworkResponse?

        constructor(networkResponse: NetworkResponse) {
            this.networkResponse = networkResponse
        }

        internal constructor() {
            this.networkResponse = null
        }

        companion object {
            @JvmField val DISABLED = WriteResult()
        }
    }

    companion object {
        /**
         * The default [CacheStrategy], which always returns the disk cache response.
         */
        @JvmField val DEFAULT: CacheStrategy = DefaultCacheStrategy()
    }
}
