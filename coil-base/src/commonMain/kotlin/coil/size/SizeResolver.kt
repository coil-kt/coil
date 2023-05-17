@file:JvmName("SizeResolvers")

package coil.size

import androidx.annotation.MainThread
import coil.request.ImageRequest

/**
 * Create a [SizeResolver] with a fixed [size].
 */
@JvmName("create")
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
}
