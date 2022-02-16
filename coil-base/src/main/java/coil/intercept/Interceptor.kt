package coil.intercept

import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.size.Scale
import coil.size.Size

/**
 * Observe, transform, short circuit, or retry requests to an [ImageLoader]'s image engine.
 */
fun interface Interceptor {

    suspend fun intercept(chain: Chain): ImageResult

    interface Chain {

        val request: ImageRequest

        val size: Size

        val scale: Scale

        /**
         * Set the requested [Size] to load the image at.
         *
         * @param size The requested size for the image.
         */
        fun withSize(size: Size): Chain

        /**
         * Set the requested [Scale]. This determines how the image is scaled to fit [size].
         *
         * @param scale The requested scaling algorithm.
         */
        fun withScale(scale: Scale): Chain

        /**
         * Continue executing the chain.
         *
         * @param request The request to proceed with.
         */
        suspend fun proceed(request: ImageRequest): ImageResult
    }
}
