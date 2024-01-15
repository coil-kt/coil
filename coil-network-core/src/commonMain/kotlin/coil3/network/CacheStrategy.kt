package coil3.network

import coil3.annotation.ExperimentalCoilApi
import coil3.network.CacheStrategy.Output
import coil3.request.Options
import kotlin.js.JsName

/**
 * The default [CacheStrategy], which always returns the disk cache response.
 */
@ExperimentalCoilApi
@JsName("newCacheStrategy")
fun CacheStrategy() = CacheStrategy { Output(it.cacheResponse) }

/**
 * Determines whether to use a cached response from the disk cache or perform a new network request.
 */
@ExperimentalCoilApi
fun interface CacheStrategy {

    suspend fun compute(input: Input): Output

    class Input(
        val cacheResponse: CacheResponse,
        val networkRequest: NetworkRequest,
        val options: Options,
    )

    class Output {
        val cacheResponse: CacheResponse?
        val networkRequest: NetworkRequest?

        /**
         * Create an output that will use [cacheResponse] as the image source.
         */
        constructor(cacheResponse: CacheResponse) {
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
            cacheResponse: CacheResponse,
            networkRequest: NetworkRequest,
        ) {
            this.cacheResponse = cacheResponse
            this.networkRequest = networkRequest
        }
    }
}
