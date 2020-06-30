package coil.interceptor

import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.request.ImageRequest
import coil.request.RequestResult
import coil.size.Scale
import coil.size.Size

/**
 * Observe, modify, short circuit, retry, or proxy requests to an [ImageLoader]'s image engine.
 */
@ExperimentalCoilApi
interface Interceptor {

    suspend fun intercept(chain: Chain): RequestResult

    interface Chain {

        val request: ImageRequest

        val size: Size

        val scale: Scale

        fun withSize(size: Size): Chain

        fun withScale(scale: Scale): Chain

        /**
         * Continue executing the chain.
         *
         * @param request The request to proceed with.
         */
        suspend fun proceed(request: ImageRequest): RequestResult
    }
}
