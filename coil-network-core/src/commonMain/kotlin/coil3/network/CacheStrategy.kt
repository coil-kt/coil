package coil3.network

import coil3.annotation.ExperimentalCoilApi
import coil3.decode.ImageSource
import coil3.network.internal.DefaultCacheStrategy
import coil3.request.Options
import kotlin.jvm.JvmField

/**
 * Controls the behavior around reading/writing responses from/to the disk cache.
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
        val request: NetworkRequest?
        val response: NetworkResponse?

        /**
         * Launch a new network [request].
         */
        constructor(request: NetworkRequest) {
            this.request = request
            this.response = null
        }

        /**
         * Use the [response]'s body as the [ImageSource].
         */
        constructor(response: NetworkResponse) {
            this.request = null
            this.response = response
        }
    }

    class WriteResult {
        val response: NetworkResponse?

        /**
         * Write [response] to the disk cache. Set [NetworkResponse.body] to `null` to skip
         * writing the response body.
         */
        constructor(response: NetworkResponse) {
            this.response = response
        }

        internal constructor() {
            this.response = null
        }

        companion object {
            /**
             * Do not write anything to the disk cache.
             */
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
