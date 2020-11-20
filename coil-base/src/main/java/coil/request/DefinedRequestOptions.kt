@file:Suppress("unused")

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
class DefinedRequestOptions(
    val lifecycle: Lifecycle?,
    val sizeResolver: SizeResolver?,
    val scale: Scale?,
    val dispatcher: CoroutineDispatcher?,
    val transition: Transition?,
    val precision: Precision?,
    val bitmapConfig: Bitmap.Config?,
    val allowHardware: Boolean?,
    val allowRgb565: Boolean?,
    val memoryCachePolicy: CachePolicy?,
    val diskCachePolicy: CachePolicy?,
    val networkCachePolicy: CachePolicy?
) {

    fun copy(
        lifecycle: Lifecycle? = this.lifecycle,
        sizeResolver: SizeResolver? = this.sizeResolver,
        scale: Scale? = this.scale,
        dispatcher: CoroutineDispatcher? = this.dispatcher,
        transition: Transition? = this.transition,
        precision: Precision? = this.precision,
        bitmapConfig: Bitmap.Config? = this.bitmapConfig,
        allowHardware: Boolean? = this.allowHardware,
        allowRgb565: Boolean? = this.allowRgb565,
        memoryCachePolicy: CachePolicy? = this.memoryCachePolicy,
        diskCachePolicy: CachePolicy? = this.diskCachePolicy,
        networkCachePolicy: CachePolicy? = this.networkCachePolicy
    ) = DefinedRequestOptions(lifecycle, sizeResolver, scale, dispatcher, transition, precision, bitmapConfig,
        allowHardware, allowRgb565, memoryCachePolicy, diskCachePolicy, networkCachePolicy)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is DefinedRequestOptions &&
            lifecycle == other.lifecycle &&
            sizeResolver == other.sizeResolver &&
            scale == other.scale &&
            dispatcher == other.dispatcher &&
            transition == other.transition &&
            precision == other.precision &&
            bitmapConfig == other.bitmapConfig &&
            allowHardware == other.allowHardware &&
            allowRgb565 == other.allowRgb565 &&
            memoryCachePolicy == other.memoryCachePolicy &&
            diskCachePolicy == other.diskCachePolicy &&
            networkCachePolicy == other.networkCachePolicy
    }

    override fun hashCode(): Int {
        var result = lifecycle?.hashCode() ?: 0
        result = 31 * result + (sizeResolver?.hashCode() ?: 0)
        result = 31 * result + (scale?.hashCode() ?: 0)
        result = 31 * result + (dispatcher?.hashCode() ?: 0)
        result = 31 * result + (transition?.hashCode() ?: 0)
        result = 31 * result + (precision?.hashCode() ?: 0)
        result = 31 * result + (bitmapConfig?.hashCode() ?: 0)
        result = 31 * result + (allowHardware?.hashCode() ?: 0)
        result = 31 * result + (allowRgb565?.hashCode() ?: 0)
        result = 31 * result + (memoryCachePolicy?.hashCode() ?: 0)
        result = 31 * result + (diskCachePolicy?.hashCode() ?: 0)
        result = 31 * result + (networkCachePolicy?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "DefinedRequestOptions(lifecycle=$lifecycle, sizeResolver=$sizeResolver, scale=$scale, " +
            "dispatcher=$dispatcher, transition=$transition, precision=$precision, bitmapConfig=$bitmapConfig, " +
            "allowHardware=$allowHardware, allowRgb565=$allowRgb565, memoryCachePolicy=$memoryCachePolicy, " +
            "diskCachePolicy=$diskCachePolicy, networkCachePolicy=$networkCachePolicy)"
    }
}
