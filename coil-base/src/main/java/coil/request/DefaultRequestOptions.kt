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
class DefaultRequestOptions(
    val interceptorDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    val fetcherDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val decoderDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val transformationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val transitionFactory: Transition.Factory = Transition.Factory.NONE,
    val precision: Precision = Precision.AUTOMATIC,
    val bitmapConfig: Bitmap.Config = Utils.DEFAULT_BITMAP_CONFIG,
    val allowHardware: Boolean = true,
    val allowRgb565: Boolean = false,
    val placeholder: Drawable? = null,
    val error: Drawable? = null,
    val fallback: Drawable? = null,
    val memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,
    val diskCachePolicy: CachePolicy = CachePolicy.ENABLED,
    val networkCachePolicy: CachePolicy = CachePolicy.ENABLED,
) {

    fun copy(
        interceptorDispatcher: CoroutineDispatcher = this.interceptorDispatcher,
        fetcherDispatcher: CoroutineDispatcher = this.fetcherDispatcher,
        decoderDispatcher: CoroutineDispatcher = this.decoderDispatcher,
        transformationDispatcher: CoroutineDispatcher = this.transformationDispatcher,
        transitionFactory: Transition.Factory = this.transitionFactory,
        precision: Precision = this.precision,
        bitmapConfig: Bitmap.Config = this.bitmapConfig,
        allowHardware: Boolean = this.allowHardware,
        allowRgb565: Boolean = this.allowRgb565,
        placeholder: Drawable? = this.placeholder,
        error: Drawable? = this.error,
        fallback: Drawable? = this.fallback,
        memoryCachePolicy: CachePolicy = this.memoryCachePolicy,
        diskCachePolicy: CachePolicy = this.diskCachePolicy,
        networkCachePolicy: CachePolicy = this.networkCachePolicy,
    ) = DefaultRequestOptions(
        interceptorDispatcher = interceptorDispatcher,
        fetcherDispatcher = fetcherDispatcher,
        decoderDispatcher = decoderDispatcher,
        transformationDispatcher = transformationDispatcher,
        transitionFactory = transitionFactory,
        precision = precision,
        bitmapConfig = bitmapConfig,
        allowHardware = allowHardware,
        allowRgb565 = allowRgb565,
        placeholder = placeholder,
        error = error,
        fallback = fallback,
        memoryCachePolicy = memoryCachePolicy,
        diskCachePolicy = diskCachePolicy,
        networkCachePolicy = networkCachePolicy,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is DefaultRequestOptions &&
            interceptorDispatcher == other.interceptorDispatcher &&
            fetcherDispatcher == other.fetcherDispatcher &&
            decoderDispatcher == other.decoderDispatcher &&
            transformationDispatcher == other.transformationDispatcher &&
            transitionFactory == other.transitionFactory &&
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
        var result = interceptorDispatcher.hashCode()
        result = 31 * result + fetcherDispatcher.hashCode()
        result = 31 * result + decoderDispatcher.hashCode()
        result = 31 * result + transformationDispatcher.hashCode()
        result = 31 * result + transitionFactory.hashCode()
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
}
