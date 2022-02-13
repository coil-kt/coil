@file:JvmName("ScaleResolvers")

package coil.size

import androidx.annotation.MainThread
import coil.request.ImageRequest

/**
 * Create a [ScaleResolver] with a fixed [scale].
 */
@JvmName("create")
fun ScaleResolver(scale: Scale): ScaleResolver = RealScaleResolver(scale)

/**
 * An interface for measuring the target scale for an image request.
 *
 * @see ImageRequest.Builder.scale
 */
fun interface ScaleResolver {

    /** Return the [Scale] that the image should be loaded with. */
    @MainThread
    suspend fun scale(): Scale
}
