package coil

import android.graphics.drawable.Drawable
import coil.request.RequestBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * A set of default request options, which are used to initialize a [RequestBuilder].
 *
 * @see ImageLoader.defaults
 */
data class DefaultRequestOptions(
    val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    val allowRgb565: Boolean = false,
    val crossfadeMillis: Int = 0,
    val placeholder: Drawable? = null,
    val error: Drawable? = null
)
