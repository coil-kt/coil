package coil3.network

import coil3.annotation.ExperimentalCoilApi
import coil3.request.Options
import kotlin.jvm.JvmField

/**
 * Determines whether to use a cached response from the disk cache and/or perform a new network request.
 */
@ExperimentalCoilApi
interface CacheStrategy {

    suspend fun allowRead(
        networkRequest: NetworkRequest,
        cacheResponse: NetworkResponse,
        options: Options,
    ): ReadResult

    suspend fun allowWrite(
        networkRequest: NetworkRequest,
        networkResponse: NetworkResponse,
        options: Options,
    ): NetworkResponse?

    class ReadResult {
        val cacheResponse: NetworkResponse?
        val networkRequest: NetworkRequest?

        /**
         * Create an output that will use [cacheResponse] as the image source.
         */
        constructor(cacheResponse: NetworkResponse) {
            this.cacheResponse = cacheResponse
            this.networkRequest = null
        }

        /**
         * Create an output that will execute [networkRequest] and use the response body as the
         * image source.
         */
        constructor(networkRequest: NetworkRequest) {
            this.cacheResponse = null
            this.networkRequest = networkRequest
        }

        /**
         * Create an output that will execute [networkRequest] and use [cacheResponse] as the image
         * source if the response code is 304 (not modified). Else, the network request's response
         * body will be used as the image source.
         */
        constructor(
            cacheResponse: NetworkResponse,
            networkRequest: NetworkRequest,
        ) {
            this.cacheResponse = cacheResponse
            this.networkRequest = networkRequest
        }
    }

    sealed interface WriteResult {

    }

    companion object {
        /**
         * The default [CacheStrategy], which always returns the disk cache response.
         */
        @JvmField val DEFAULT = object : CacheStrategy {
            override suspend fun allowRead(
                networkRequest: NetworkRequest,
                cacheResponse: NetworkResponse,
                options: Options,
            ) = ReadResult(cacheResponse)

            override suspend fun allowWrite(
                networkRequest: NetworkRequest,
                networkResponse: NetworkResponse,
                options: Options,
            ) = networkResponse
        }
    }
}
