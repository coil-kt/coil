package coil3.compose

import androidx.compose.runtime.Stable
import coil3.request.ImageRequest

/**
 * Determines equality between two values or a value's hash code.
 */
@Stable
interface EqualityDelegate {
    fun equals(self: Any?, other: Any?): Boolean
    fun hashCode(self: Any?): Int
}

/**
 * The default [EqualityDelegate] used to determine equality and the hash code for the `model`
 * argument for [rememberAsyncImagePainter], [AsyncImage], and [SubcomposeAsyncImage].
 */
val DefaultModelEqualityDelegate = object : EqualityDelegate {

    override fun equals(self: Any?, other: Any?): Boolean {
        if (self !is ImageRequest || other !is ImageRequest) {
            return self == other
        }

        return self.context == other.context &&
            self.data == other.data &&
            self.memoryCacheKey == other.memoryCacheKey &&
            self.memoryCacheKeyExtras == other.memoryCacheKeyExtras &&
            self.diskCacheKey == other.diskCacheKey &&
            self.fileSystem == other.fileSystem &&
            self.sizeResolver == other.sizeResolver &&
            self.scale == other.scale &&
            self.precision == other.precision
    }

    override fun hashCode(self: Any?): Int {
        if (self !is ImageRequest) {
            return self.hashCode()
        }

        var result = self.context.hashCode()
        result = 31 * result + self.data.hashCode()
        result = 31 * result + self.memoryCacheKey.hashCode()
        result = 31 * result + self.memoryCacheKeyExtras.hashCode()
        result = 31 * result + self.diskCacheKey.hashCode()
        result = 31 * result + self.fileSystem.hashCode()
        result = 31 * result + self.sizeResolver.hashCode()
        result = 31 * result + self.scale.hashCode()
        result = 31 * result + self.precision.hashCode()
        return result
    }
}
