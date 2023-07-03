package coil.request

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import coil.Image
import coil.ImageLoader
import coil.size.Precision
import coil.transition.Transition
import coil.util.DEFAULT_BITMAP_CONFIG
import coil.util.ioCoroutineDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * A set of default options that are used to fill in unset [ImageRequest] values.
 *
 * @see ImageLoader.defaults
 * @see ImageRequest.defaults
 */
data class DefaultRequestOptions(
    val interceptorDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    val fetcherDispatcher: CoroutineDispatcher = ioCoroutineDispatcher(),
    val decoderDispatcher: CoroutineDispatcher = ioCoroutineDispatcher(),
    val transformationDispatcher: CoroutineDispatcher = ioCoroutineDispatcher(),
    val transitionFactory: Transition.Factory = Transition.Factory.NONE,
    val precision: Precision = Precision.AUTOMATIC,
    val bitmapConfig: Bitmap.Config = DEFAULT_BITMAP_CONFIG,
    val allowHardware: Boolean = true,
    val allowRgb565: Boolean = false,
    val placeholder: Image? = null,
    val error: Image? = null,
    val fallback: Image? = null,
    val memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,
    val diskCachePolicy: CachePolicy = CachePolicy.ENABLED,
    val networkCachePolicy: CachePolicy = CachePolicy.ENABLED,
)
