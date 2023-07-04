package coil.request

import android.graphics.Bitmap
import androidx.lifecycle.Lifecycle
import coil.size.Precision
import coil.size.Scale
import coil.size.SizeResolver
import coil.transition.Transition
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Tracks which values have been set (instead of computed automatically using a default)
 * when building an [ImageRequest].
 *
 * @see ImageRequest.defined
 */
data class DefinedRequestOptions(
    val lifecycle: Lifecycle?,
    val sizeResolver: SizeResolver?,
    val scale: Scale?,
    val interceptorDispatcher: CoroutineDispatcher?,
    val fetcherDispatcher: CoroutineDispatcher?,
    val decoderDispatcher: CoroutineDispatcher?,
    val transitionFactory: Transition.Factory?,
    val precision: Precision?,
    val bitmapConfig: Bitmap.Config?,
    val allowHardware: Boolean?,
    val allowRgb565: Boolean?,
    val memoryCachePolicy: CachePolicy?,
    val diskCachePolicy: CachePolicy?,
    val networkCachePolicy: CachePolicy?,
)
