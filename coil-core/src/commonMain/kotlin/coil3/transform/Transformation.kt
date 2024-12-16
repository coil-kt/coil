package coil3.transform

import coil3.Bitmap
import coil3.BitmapImage
import coil3.decode.DecodeResult
import coil3.fetch.ImageFetchResult
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Size

/**
 * An interface for making transformations to an image's pixel data.
 *
 * NOTE: If [ImageFetchResult.image] or [DecodeResult.image] is not a [BitmapImage],
 * it will be converted to one. This will cause animated drawables to only draw the first frame of
 * their animation.
 *
 * @see ImageRequest.Builder.transformations
 */
abstract class Transformation {

    /**
     * The unique cache key for this transformation.
     *
     * The key is added to the image request's memory cache key and should contain any params that
     * are part of this transformation (e.g. size, scale, color, radius, etc.).
     */
    abstract val cacheKey: String

    /**
     * Apply the transformation to [input] and return the transformed [Bitmap].
     *
     * @param input The input [Bitmap] to transform.
     * @param size The size of the image request.
     * @return The transformed [Bitmap].
     */
    abstract suspend fun transform(input: Bitmap, size: Size): Bitmap

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is Transformation && cacheKey == other.cacheKey
    }

    override fun hashCode(): Int {
        return cacheKey.hashCode()
    }

    override fun toString(): String {
        return "${this::class.simpleName}(cacheKey=$cacheKey)"
    }
}
