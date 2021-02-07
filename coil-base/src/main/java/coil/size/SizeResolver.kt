package coil.size

import androidx.annotation.MainThread
import coil.request.ImageRequest

/**
 * An interface for measuring the target size for an image request.
 *
 * @see ImageRequest.Builder.size
 */
public interface SizeResolver {

    public companion object {
        /** Create a [SizeResolver] with a fixed [size]. */
        @JvmStatic
        @JvmName("create")
        public operator fun invoke(size: Size): SizeResolver = RealSizeResolver(size)
    }

    /** Return the [Size] that the image should be loaded at. */
    @MainThread
    public suspend fun size(): Size
}
