package coil.intercept

import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.size.Size

/**
 * Observe, transform, short circuit, or retry requests to an [ImageLoader]'s image engine.
 *
 * NOTE: The interceptor chain is launched from the main thread by default.
 * See [ImageLoader.Builder.launchInterceptorChainOnMainThread] for more information.
 */
@ExperimentalCoilApi
public interface Interceptor {

    public suspend fun intercept(chain: Chain): ImageResult

    public interface Chain {

        public val request: ImageRequest

        public val size: Size

        /**
         * Set the requested [Size] to load the image at.
         *
         * @param size The requested size for the image.
         */
        public fun withSize(size: Size): Chain

        /**
         * Continue executing the chain.
         *
         * @param request The request to proceed with.
         */
        public suspend fun proceed(request: ImageRequest): ImageResult
    }
}
