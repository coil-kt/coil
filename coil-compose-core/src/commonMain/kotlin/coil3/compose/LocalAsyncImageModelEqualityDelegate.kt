package coil3.compose

import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import coil3.annotation.ExperimentalCoilApi
import coil3.request.ImageRequest
import kotlin.jvm.JvmField

@ExperimentalCoilApi
val LocalAsyncImageModelEqualityDelegate = staticCompositionLocalOf {
    AsyncImageModelEqualityDelegate.Default
}

/**
 * Determines equality between two models.
 *
 * This allows you to control when [rememberAsyncImagePainter], [AsyncImage], and
 * [SubcomposeAsyncImage] execute a new request and recompose due to a model change.
 */
@ExperimentalCoilApi
@Stable
interface AsyncImageModelEqualityDelegate {
    fun equals(self: Any?, other: Any?): Boolean
    fun hashCode(self: Any?): Int

    companion object {
        /**
         * The default model equality delegate that only compares a subset of [ImageRequest]'s
         * properties instead of delegating to its own [equals] and [hashCode] implementations.
         */
        @JvmField val Default = object : AsyncImageModelEqualityDelegate {

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

            override fun toString(): String {
                return "AsyncImageModelEqualityDelegate.Default"
            }
        }

        /**
         * A model equality delegate that always delegates to the model's [equals] and [hashCode].
         * If the model is an [ImageRequest] this will compare all of its properties and this may
         * cause recompositions than necessary. Consider [remember]ing your [ImageRequest]s if you
         * use this model equality delegate.
         */
        @JvmField val AllProperties = object : AsyncImageModelEqualityDelegate {
            override fun equals(self: Any?, other: Any?) = self == other
            override fun hashCode(self: Any?) = self.hashCode()
            override fun toString() = "AsyncImageModelEqualityDelegate.AllProperties"
        }
    }
}
