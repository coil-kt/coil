package coil.interceptor

import coil.DefaultRequestOptions
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.request.ImageRequest
import coil.request.RequestResult
import coil.size.Size

/**
 * Observe, modify, short circuit, retry, or wrap requests to an [ImageLoader]'s image engine.
 */
@ExperimentalCoilApi
interface Interceptor {

    suspend fun intercept(chain: Chain): RequestResult

    interface Chain {

        val request: ImageRequest

        val defaults: DefaultRequestOptions

        val size: Size

        /**
         * Modify the [size] passed to subsequent interceptors in the chain.
         *
         * @see size The resolved size for the request.
         */
        fun withSize(size: Size): Chain

        /**
         * Continue executing the chain.
         *
         * @param request The request to proceed with.
         */
        suspend fun proceed(request: ImageRequest): RequestResult
    }
}
