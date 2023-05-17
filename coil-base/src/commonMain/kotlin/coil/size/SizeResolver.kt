// https://youtrack.jetbrains.com/issue/KTIJ-7642
@file:Suppress("FUN_INTERFACE_WITH_SUSPEND_FUNCTION")
@file:JvmName("SizeResolvers")

package coil.size

import coil.annotation.MainThread
import coil.request.ImageRequest
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
