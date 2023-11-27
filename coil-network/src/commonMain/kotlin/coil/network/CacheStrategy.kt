package coil.network

import coil.annotation.ExperimentalCoilApi
import coil.network.CacheStrategy.Output
import coil.request.Options
import io.ktor.client.request.HttpRequestBuilder
import kotlin.js.JsName

/**
 * The default [CacheStrategy], which always returns the disk cache response.
 */
@ExperimentalCoilApi
@JsName("newCacheStrategy")
fun CacheStrategy() = CacheStrategy { Output(it.cacheResponse) }

@ExperimentalCoilApi
fun interface CacheStrategy {

    fun compute(input: Input): Output

    class Input(
        val url: String,
        val options: Options,
        val cacheResponse: CacheResponse,
    )

    class Output {
        val cacheResponse: CacheResponse?
        val networkRequest: HttpRequestBuilder?

        constructor(diskCacheResponse: CacheResponse) {
            this.cacheResponse = diskCacheResponse
            this.networkRequest = null
        }

        constructor(networkRequest: HttpRequestBuilder) {
            this.cacheResponse = null
            this.networkRequest = networkRequest
        }

        constructor(
            diskCacheResponse: CacheResponse,
            networkRequest: HttpRequestBuilder,
        ) {
            this.cacheResponse = diskCacheResponse
            this.networkRequest = networkRequest
        }
    }
}
