package coil.compose

import androidx.compose.runtime.Stable
import coil.request.ImageRequest

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
        if (self === other) {
            return true
        }

        if (self !is ImageRequest || other !is ImageRequest) {
            return self == other
        }

        return self.context == other.context &&
            self.data == other.data &&
            self.placeholderMemoryCacheKey == other.placeholderMemoryCacheKey &&
            self.memoryCacheKey == other.memoryCacheKey &&
            self.diskCacheKey == other.diskCacheKey &&
            self.bitmapConfig == other.bitmapConfig &&
            self.colorSpace == other.colorSpace &&
            self.transformations == other.transformations &&
            self.headers == other.headers &&
            self.allowConversionToBitmap == other.allowConversionToBitmap &&
            self.allowHardware == other.allowHardware &&
            self.allowRgb565 == other.allowRgb565 &&
            self.premultipliedAlpha == other.premultipliedAlpha &&
            self.memoryCachePolicy == other.memoryCachePolicy &&
            self.diskCachePolicy == other.diskCachePolicy &&
            self.networkCachePolicy == other.networkCachePolicy &&
            self.sizeResolver == other.sizeResolver &&
            self.scale == other.scale &&
            self.precision == other.precision &&
            self.parameters == other.parameters
    }

    override fun hashCode(self: Any?): Int {
        if (self !is ImageRequest) {
            return self.hashCode()
        }

        var result = self.context.hashCode()
        result = 31 * result + self.data.hashCode()
        result = 31 * result + self.placeholderMemoryCacheKey.hashCode()
        result = 31 * result + self.memoryCacheKey.hashCode()
        result = 31 * result + self.diskCacheKey.hashCode()
        result = 31 * result + self.bitmapConfig.hashCode()
        result = 31 * result + self.colorSpace.hashCode()
        result = 31 * result + self.transformations.hashCode()
        result = 31 * result + self.headers.hashCode()
        result = 31 * result + self.allowConversionToBitmap.hashCode()
        result = 31 * result + self.allowHardware.hashCode()
        result = 31 * result + self.allowRgb565.hashCode()
        result = 31 * result + self.premultipliedAlpha.hashCode()
        result = 31 * result + self.memoryCachePolicy.hashCode()
        result = 31 * result + self.diskCachePolicy.hashCode()
        result = 31 * result + self.networkCachePolicy.hashCode()
        result = 31 * result + self.sizeResolver.hashCode()
        result = 31 * result + self.scale.hashCode()
        result = 31 * result + self.precision.hashCode()
        result = 31 * result + self.parameters.hashCode()
        return result
    }
}
