package coil3.intercept

import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.size.Size

/**
 * Observe, transform, short circuit, or retry requests to an [ImageLoader]'s image engine.
 */
fun interface Interceptor {

    suspend fun intercept(chain: Chain): ImageResult

    interface Chain {

        val request: ImageRequest

        val size: Size

        /**
         * Copy the current [Chain] and replace [request].
         *
         * This method is similar to [proceed] except it doesn't advance the chain to the
         * next interceptor.
         *
         * @param request The current image request.
         */
        fun withRequest(request: ImageRequest): Chain

        /**
         * Copy the current [Chain] and replace [size].
         *
         * Use this method to replace the resolved size for this image request.
         *
         * @param size The requested size for the image.
         */
        fun withSize(size: Size): Chain

        /**
         * Continue executing the chain.
         */
        suspend fun proceed(): ImageResult
    }
}
