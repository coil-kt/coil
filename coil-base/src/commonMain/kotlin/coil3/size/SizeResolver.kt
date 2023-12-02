// https://youtrack.jetbrains.com/issue/KTIJ-7642
@file:Suppress("FUN_INTERFACE_WITH_SUSPEND_FUNCTION")

package coil3.size

import coil3.annotation.MainThread
import coil3.request.ImageRequest
import kotlin.jvm.JvmName

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
