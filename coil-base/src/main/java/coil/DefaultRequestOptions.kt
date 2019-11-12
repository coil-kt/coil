@file:UseExperimental(ExperimentalCoil::class)

package coil

import android.graphics.drawable.Drawable
import coil.annotation.ExperimentalCoil
import coil.request.RequestBuilder
import coil.transition.Transition
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * A set of default request options, which are used to initialize a [RequestBuilder].
 *
 * @see ImageLoader.defaults
 */
data class DefaultRequestOptions(
    val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    val transitionFactory: Transition.Factory? = null,
    val allowHardware: Boolean = true,
    val allowRgb565: Boolean = false,
    val placeholder: Drawable? = null,
    val error: Drawable? = null
)
