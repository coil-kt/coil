@file:OptIn(ExperimentalCoilApi::class)

package coil

import android.graphics.drawable.Drawable
import coil.annotation.ExperimentalCoilApi
import coil.request.CachePolicy
import coil.request.Request
import coil.size.Precision
import coil.transition.Transition
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * A request options which are used as default values for unset [Request] options.
 *
 * @see ImageLoader.defaults
 */
data class DefaultRequestOptions(
    val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    val transition: Transition = Transition.NONE,
    val precision: Precision = Precision.AUTOMATIC,
    val allowHardware: Boolean = true,
    val allowRgb565: Boolean = false,
    val placeholder: Drawable? = null,
    val error: Drawable? = null,
    val fallback: Drawable? = null,
    val memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,
    val diskCachePolicy: CachePolicy = CachePolicy.ENABLED,
    val networkCachePolicy: CachePolicy = CachePolicy.ENABLED
)
