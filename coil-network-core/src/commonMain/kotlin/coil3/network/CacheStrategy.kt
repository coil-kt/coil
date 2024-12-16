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

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is ReadResult &&
                request == other.request &&
                response == other.response
        }

        override fun hashCode(): Int {
            var result = request?.hashCode() ?: 0
            result = 31 * result + (response?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "ReadResult(request=$request, response=$response)"
        }
    }

    class WriteResult {
        val response: NetworkResponse?

        /**
         * Write [response] to the disk cache.
         * Set [NetworkResponse.body] to `null` to skip writing the response body.
         */
        constructor(response: NetworkResponse) {
            this.response = response
        }

        /**
         * Use [DISABLED] instead of this constructor.
         */
        private constructor() {
            this.response = null
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is WriteResult && response == other.response
        }

        override fun hashCode(): Int {
            return response?.hashCode() ?: 0
        }

        override fun toString(): String {
            return "WriteResult(response=$response)"
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
