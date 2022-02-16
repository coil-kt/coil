package coil.request

import android.graphics.Bitmap
import androidx.lifecycle.Lifecycle
import coil.size.Precision
import coil.size.ScaleResolver
import coil.size.SizeResolver
import coil.transition.Transition
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Tracks which values have been set (instead of computed automatically using a default)
 * when building an [ImageRequest].
 *
 * @see ImageRequest.defined
 */
class DefinedRequestOptions(
    val lifecycle: Lifecycle?,
    val sizeResolver: SizeResolver?,
    val scaleResolver: ScaleResolver?,
    val interceptorDispatcher: CoroutineDispatcher?,
    val fetcherDispatcher: CoroutineDispatcher?,
    val decoderDispatcher: CoroutineDispatcher?,
    val transformationDispatcher: CoroutineDispatcher?,
    val transitionFactory: Transition.Factory?,
    val precision: Precision?,
    val bitmapConfig: Bitmap.Config?,
    val allowHardware: Boolean?,
    val allowRgb565: Boolean?,
    val memoryCachePolicy: CachePolicy?,
    val diskCachePolicy: CachePolicy?,
    val networkCachePolicy: CachePolicy?,
) {

    fun copy(
        lifecycle: Lifecycle? = this.lifecycle,
        sizeResolver: SizeResolver? = this.sizeResolver,
        scaleResolver: ScaleResolver? = this.scaleResolver,
        interceptorDispatcher: CoroutineDispatcher? = this.interceptorDispatcher,
        fetcherDispatcher: CoroutineDispatcher? = this.fetcherDispatcher,
        decoderDispatcher: CoroutineDispatcher? = this.decoderDispatcher,
        transformationDispatcher: CoroutineDispatcher? = this.transformationDispatcher,
        transitionFactory: Transition.Factory? = this.transitionFactory,
        precision: Precision? = this.precision,
        bitmapConfig: Bitmap.Config? = this.bitmapConfig,
        allowHardware: Boolean? = this.allowHardware,
        allowRgb565: Boolean? = this.allowRgb565,
        memoryCachePolicy: CachePolicy? = this.memoryCachePolicy,
        diskCachePolicy: CachePolicy? = this.diskCachePolicy,
        networkCachePolicy: CachePolicy? = this.networkCachePolicy,
    ) = DefinedRequestOptions(
        lifecycle = lifecycle,
        sizeResolver = sizeResolver,
        scaleResolver = scaleResolver,
        interceptorDispatcher = interceptorDispatcher,
        fetcherDispatcher = fetcherDispatcher,
        decoderDispatcher = decoderDispatcher,
        transformationDispatcher = transformationDispatcher,
        transitionFactory = transitionFactory,
        precision = precision,
        bitmapConfig = bitmapConfig,
        allowHardware = allowHardware,
        allowRgb565 = allowRgb565,
        memoryCachePolicy = memoryCachePolicy,
        diskCachePolicy = diskCachePolicy,
        networkCachePolicy = networkCachePolicy,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is DefinedRequestOptions &&
            lifecycle == other.lifecycle &&
            sizeResolver == other.sizeResolver &&
            scaleResolver == other.scaleResolver &&
            interceptorDispatcher == other.interceptorDispatcher &&
            fetcherDispatcher == other.fetcherDispatcher &&
            decoderDispatcher == other.decoderDispatcher &&
            transformationDispatcher == other.transformationDispatcher &&
            transitionFactory == other.transitionFactory &&
            precision == other.precision &&
            bitmapConfig == other.bitmapConfig &&
            allowHardware == other.allowHardware &&
            allowRgb565 == other.allowRgb565 &&
            memoryCachePolicy == other.memoryCachePolicy &&
            diskCachePolicy == other.diskCachePolicy &&
            networkCachePolicy == other.networkCachePolicy
    }

    override fun hashCode(): Int {
        var result = lifecycle.hashCode()
        result = 31 * result + sizeResolver.hashCode()
        result = 31 * result + scaleResolver.hashCode()
        result = 31 * result + interceptorDispatcher.hashCode()
        result = 31 * result + fetcherDispatcher.hashCode()
        result = 31 * result + decoderDispatcher.hashCode()
        result = 31 * result + transformationDispatcher.hashCode()
        result = 31 * result + transitionFactory.hashCode()
        result = 31 * result + precision.hashCode()
        result = 31 * result + bitmapConfig.hashCode()
        result = 31 * result + allowHardware.hashCode()
        result = 31 * result + allowRgb565.hashCode()
        result = 31 * result + memoryCachePolicy.hashCode()
        result = 31 * result + diskCachePolicy.hashCode()
        result = 31 * result + networkCachePolicy.hashCode()
        return result
    }
}
