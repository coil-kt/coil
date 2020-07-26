package coil.size

import androidx.annotation.MainThread
import coil.request.ImageRequest

/**
 * An interface for measuring the target size for an image request.
 *
 * @see ImageRequest.Builder.size
 */
interface SizeResolver {

    companion object {
        /** Create a [SizeResolver] with a fixed [size]. */
        @JvmStatic
        @JvmName("create")
        operator fun invoke(size: Size): SizeResolver = RealSizeResolver(size)
    }

    /** Return the [Size] that the image should be loaded at. */
    @MainThread
    suspend fun size(): Size
}
