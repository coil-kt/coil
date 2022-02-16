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
 * An interface for lazily determining the [Scale] for an image request.
 *
 * @see ImageRequest.Builder.scale
 */
fun interface ScaleResolver {

    /** Return the resolved [Scale]. */
    @MainThread
    fun scale(): Scale
}
