package coil.transform

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.bitmap.BitmapPool
import coil.decode.DecodeResult
import coil.fetch.DrawableResult
import coil.request.ImageRequest
import coil.size.Size

/**
 * An interface for making transformations to an image's pixel data.
 *
 * NOTE: If [DrawableResult.drawable] or [DecodeResult.drawable] is not a [BitmapDrawable], it will be converted to one.
 * This will cause animated drawables to only draw the first frame of their animation.
 *
 * @see ImageRequest.Builder.transformations
 */
interface Transformation {

    /**
     * Return a unique key for this transformation.
     *
     * The key is added to the image request's memory cache key and should contain any params that are part of this
     * transformation (e.g. size, scale, color, radius, etc.).
     */
    fun key(): String

    /**
     * Apply the transformation to [input] and return the transformed [Bitmap].
     *
     * @param pool A [BitmapPool] which can be used to request [Bitmap] instances.
     * @param input The input [Bitmap] to transform. Its config will always be [Bitmap.Config.ARGB_8888] or [Bitmap.Config.RGBA_F16].
     * @param size The size of the image request.
     * @return The transformed [Bitmap].
     */
    suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size): Bitmap
}
