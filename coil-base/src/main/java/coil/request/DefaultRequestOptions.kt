@file:Suppress("unused")

package coil.request

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.size.Precision
import coil.transition.Transition
import coil.util.Utils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * A set of default options that are used to fill in unset [ImageRequest] values.
 *
 * @see ImageLoader.defaults
 * @see ImageRequest.defaults
 */
public class DefaultRequestOptions(
    public val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    public val transition: Transition = Transition.NONE,
    public val precision: Precision = Precision.AUTOMATIC,
    public val bitmapConfig: Bitmap.Config = Utils.DEFAULT_BITMAP_CONFIG,
    public val allowHardware: Boolean = true,
    public val allowRgb565: Boolean = false,
    public val placeholder: Drawable? = null,
    public val error: Drawable? = null,
    public val fallback: Drawable? = null,
    public val memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,
    public val diskCachePolicy: CachePolicy = CachePolicy.ENABLED,
    public val networkCachePolicy: CachePolicy = CachePolicy.ENABLED
) {

    public fun copy(
        dispatcher: CoroutineDispatcher = this.dispatcher,
        transition: Transition = this.transition,
        precision: Precision = this.precision,
        bitmapConfig: Bitmap.Config = this.bitmapConfig,
        allowHardware: Boolean = this.allowHardware,
        allowRgb565: Boolean = this.allowRgb565,
        placeholder: Drawable? = this.placeholder,
        error: Drawable? = this.error,
        fallback: Drawable? = this.fallback,
        memoryCachePolicy: CachePolicy = this.memoryCachePolicy,
        diskCachePolicy: CachePolicy = this.diskCachePolicy,
        networkCachePolicy: CachePolicy = this.networkCachePolicy
    ): DefaultRequestOptions = DefaultRequestOptions(dispatcher, transition, precision, bitmapConfig, allowHardware, allowRgb565, placeholder,
        error, fallback, memoryCachePolicy, diskCachePolicy, networkCachePolicy)

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

    public companion object {
        @JvmField
        public val INSTANCE: DefaultRequestOptions = DefaultRequestOptions()
    }
}
