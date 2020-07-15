@file:OptIn(ExperimentalCoilApi::class)

package coil

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import coil.annotation.ExperimentalCoilApi
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.transition.Transition
import coil.util.Utils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * A set of default options that are used to fill in unset [ImageRequest] values.
 *
 * @see ImageLoader.defaults
 */
class DefaultRequestOptions(
    val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    val transition: Transition = Transition.NONE,
    val precision: Precision = Precision.AUTOMATIC,
    val bitmapConfig: Bitmap.Config = Utils.DEFAULT_BITMAP_CONFIG,
    val allowHardware: Boolean = true,
    val allowRgb565: Boolean = false,
    val placeholder: Drawable? = null,
    val error: Drawable? = null,
    val fallback: Drawable? = null,
    val memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,
    val diskCachePolicy: CachePolicy = CachePolicy.ENABLED,
    val networkCachePolicy: CachePolicy = CachePolicy.ENABLED
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is DefaultRequestOptions &&
            dispatcher == other.dispatcher &&
            transition == other.transition &&
            precision == other.precision &&
            bitmapConfig == other.bitmapConfig &&
            allowHardware == other.allowHardware &&
            allowRgb565 == other.allowRgb565 &&
            placeholder == other.placeholder &&
            error == other.error &&
            fallback == other.fallback &&
            memoryCachePolicy == other.memoryCachePolicy &&
            diskCachePolicy == other.diskCachePolicy &&
            networkCachePolicy == other.networkCachePolicy
    }

    override fun hashCode(): Int {
        var result = dispatcher.hashCode()
        result = 31 * result + transition.hashCode()
        result = 31 * result + precision.hashCode()
        result = 31 * result + bitmapConfig.hashCode()
        result = 31 * result + allowHardware.hashCode()
        result = 31 * result + allowRgb565.hashCode()
        result = 31 * result + (placeholder?.hashCode() ?: 0)
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + (fallback?.hashCode() ?: 0)
        result = 31 * result + memoryCachePolicy.hashCode()
        result = 31 * result + diskCachePolicy.hashCode()
        result = 31 * result + networkCachePolicy.hashCode()
        return result
    }

    override fun toString(): String {
        return "DefaultRequestOptions(dispatcher=$dispatcher, transition=$transition, precision=$precision, " +
            "bitmapConfig=$bitmapConfig, allowHardware=$allowHardware, allowRgb565=$allowRgb565, " +
            "placeholder=$placeholder, error=$error, fallback=$fallback, memoryCachePolicy=$memoryCachePolicy, " +
            "diskCachePolicy=$diskCachePolicy, networkCachePolicy=$networkCachePolicy)"
    }
}
