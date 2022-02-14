@file:JvmName("ScaleResolvers")

package coil.size

import androidx.annotation.MainThread
import coil.annotation.ExperimentalCoilApi
import coil.request.ImageRequest

/**
 * Create a [ScaleResolver] with a fixed [scale].
 */
@ExperimentalCoilApi
@JvmName("create")
fun ScaleResolver(scale: Scale): ScaleResolver = RealScaleResolver(scale)

/**
 * An interface for determining the [Scale] for an image request.
 *
 * @see ImageRequest.Builder.scale
 */
@ExperimentalCoilApi
fun interface ScaleResolver {

    /** Return the resolved [Scale]. */
    @MainThread
    fun scale(): Scale
}
