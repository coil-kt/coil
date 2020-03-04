@file:OptIn(ExperimentalCoilApi::class)

package coil

import android.graphics.drawable.Drawable
import coil.annotation.ExperimentalCoilApi
import coil.request.RequestBuilder
import coil.size.Precision
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
    val transition: Transition? = null,
    val precision: Precision = Precision.AUTOMATIC,
    val allowHardware: Boolean = true,
    val allowRgb565: Boolean = false,
    val placeholder: Drawable? = null,
    val error: Drawable? = null,
    val fallback: Drawable? = null
)
