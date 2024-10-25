package coil3.compose

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import coil3.annotation.ExperimentalCoilApi
import coil3.request.ImageRequest
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

@ExperimentalCoilApi
val LocalAsyncImageModelEqualityDelegate = staticCompositionLocalOf {
    AsyncImageModelEqualityDelegate.Default
}

/**
 * Determines equality between two models.
 *
 * This allows you to control when [rememberAsyncImagePainter], [AsyncImage], and
 * [SubcomposeAsyncImage] execute a new request and recompose due to a `model` change.
 */
@ExperimentalCoilApi
@Stable
interface AsyncImageModelEqualityDelegate {
    fun equals(self: Any?, other: Any?): Boolean
    fun hashCode(self: Any?): Int

    companion object {
        @JvmField val Default: AsyncImageModelEqualityDelegate = object : AsyncImageModelEqualityDelegate {

            override fun equals(self: Any?, other: Any?): Boolean {
                if (this === other) return true
                return self is ImageRequest &&
                    other is ImageRequest &&
                    self.context == other.context &&
                    self.data == other.data &&
                    self.memoryCacheKey == other.memoryCacheKey &&
                    self.memoryCacheKeyExtras == other.memoryCacheKeyExtras &&
                    self.diskCacheKey == other.diskCacheKey &&
                    self.sizeResolver == other.sizeResolver &&
                    self.scale == other.scale &&
                    self.precision == other.precision
            }

            override fun hashCode(self: Any?): Int {
                if (self !is ImageRequest) return self.hashCode()
                var result = self.context.hashCode()
                result = 31 * result + self.data.hashCode()
                result = 31 * result + self.memoryCacheKey.hashCode()
                result = 31 * result + self.memoryCacheKeyExtras.hashCode()
                result = 31 * result + self.diskCacheKey.hashCode()
                result = 31 * result + self.sizeResolver.hashCode()
                result = 31 * result + self.scale.hashCode()
                result = 31 * result + self.precision.hashCode()
                return result
            }
        }
    }
}

@Deprecated(
    message = "Migrate to LocalAsyncImageModelEqualityDelegate.",
    level = DeprecationLevel.ERROR,
)
interface EqualityDelegate : AsyncImageModelEqualityDelegate {
    override fun equals(self: Any?, other: Any?): Boolean
    override fun hashCode(self: Any?): Int
}

@Deprecated(
    message = "Migrate to LocalAsyncImageModelEqualityDelegate.",
    level = DeprecationLevel.ERROR,
)
class EqualityDelegateKt {
    companion object {
        @Suppress("DEPRECATION_ERROR")
        @JvmStatic
        fun getDefaultModelEqualityDelegate(): EqualityDelegate = object : EqualityDelegate,
            AsyncImageModelEqualityDelegate by AsyncImageModelEqualityDelegate.Default {}
    }
}

@Suppress("DEPRECATION_ERROR")
@Deprecated(
    message = "Migrate to LocalAsyncImageModelEqualityDelegate.",
    level = DeprecationLevel.ERROR,
)
val DefaultModelEqualityDelegate get() = EqualityDelegateKt.getDefaultModelEqualityDelegate()
