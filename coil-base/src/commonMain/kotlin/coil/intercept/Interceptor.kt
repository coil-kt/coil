// https://youtrack.jetbrains.com/issue/KTIJ-7642
@file:Suppress("FUN_INTERFACE_WITH_SUSPEND_FUNCTION")

package coil.intercept

import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.size.Size

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

        @Deprecated(
            message = "Use 'withRequest' to create a new chain with the updated request before " +
                "calling 'proceed'.",
            replaceWith = ReplaceWith("withRequest(request).proceed()"),
        )
        suspend fun proceed(request: ImageRequest): ImageResult = withRequest(request).proceed()
    }
}
