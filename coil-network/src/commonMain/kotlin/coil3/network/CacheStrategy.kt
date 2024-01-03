// https://youtrack.jetbrains.com/issue/KTIJ-7642
@file:Suppress("FUN_INTERFACE_WITH_SUSPEND_FUNCTION")

package coil3.network

import coil3.network.CacheStrategy.Output
import coil3.request.Options
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpStatusCode.Companion.NotModified
import kotlin.js.JsName

/**
 * The default [CacheStrategy], which always returns the disk cache response.
 */
@JsName("newCacheStrategy")
fun CacheStrategy() = CacheStrategy { Output(it.cacheResponse) }

/**
 * Determines whether to use a cached response from the disk cache or perform a new network request.
 */
fun interface CacheStrategy {

    suspend fun compute(input: Input): Output

    class Input(
        val cacheResponse: CacheResponse,
        val networkRequest: HttpRequestBuilder,
        val options: Options,
    )

    class Output {
        val cacheResponse: CacheResponse?
        val networkRequest: HttpRequestBuilder?

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
        constructor(networkRequest: HttpRequestBuilder) {
            this.cacheResponse = null
            this.networkRequest = networkRequest
        }

        /**
         * Create an output that will execute [networkRequest] and use [cacheResponse] as the image
         * source if the response code is [NotModified]. Else, the network request's response body
         * will be used as the image source.
         */
        constructor(
            cacheResponse: CacheResponse,
            networkRequest: HttpRequestBuilder,
        ) {
            this.cacheResponse = cacheResponse
            this.networkRequest = networkRequest
        }
    }
}
