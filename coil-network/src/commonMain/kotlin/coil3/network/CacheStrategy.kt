package coil3.network

import coil3.annotation.ExperimentalCoilApi
import coil3.network.CacheStrategy.Output
import coil3.request.Options
import io.ktor.client.request.HttpRequestBuilder
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

    fun compute(input: Input): Output

    class Input(
        val cacheResponse: CacheResponse,
        val networkRequest: HttpRequestBuilder,
        val options: Options,
    )

    class Output {
        val cacheResponse: CacheResponse?
        val networkRequest: HttpRequestBuilder?

        constructor(cacheResponse: CacheResponse) {
            this.cacheResponse = cacheResponse
            this.networkRequest = null
        }

        constructor(networkRequest: HttpRequestBuilder) {
            this.cacheResponse = null
            this.networkRequest = networkRequest
        }

        constructor(
            cacheResponse: CacheResponse,
            networkRequest: HttpRequestBuilder,
        ) {
            this.cacheResponse = cacheResponse
            this.networkRequest = networkRequest
        }
    }
}
