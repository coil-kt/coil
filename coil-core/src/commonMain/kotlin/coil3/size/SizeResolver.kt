package coil3.size

import coil3.annotation.MainThread
import coil3.request.ImageRequest
import kotlin.jvm.JvmField

/**
 * Create a [SizeResolver] with a fixed [size].
 */
fun SizeResolver(size: Size): SizeResolver = RealSizeResolver(size)

/**
 * An interface for measuring the target size for an image request.
 *
 * @see ImageRequest.Builder.size
 */
fun interface SizeResolver {

    /** Return the [Size] that the image should be loaded at. */
    @MainThread
    suspend fun size(): Size

    companion object {
        @JvmField val ORIGINAL = SizeResolver(Size.ORIGINAL)
    }
}
