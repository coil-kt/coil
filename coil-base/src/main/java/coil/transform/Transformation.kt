package coil.transform

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import coil.bitmappool.BitmapPool
import coil.decode.DecodeResult
import coil.fetch.DrawableResult
import coil.request.RequestBuilder
import coil.size.Size

/**
 * An interface for making transformations to an image's pixel data.
 *
 * NOTE: If [DrawableResult.drawable] or [DecodeResult.drawable] is not a [BitmapDrawable], it will be converted to one.
 * This will cause animated drawables to only draw the first frame of their animation.
 *
 * @see RequestBuilder.transformations
 */
interface Transformation {

    companion object {
        /** A whitelist of valid bitmap configs for the input and output bitmaps of [transform]. */
        internal val VALID_CONFIGS = if (SDK_INT >= O) {
            arrayOf(Bitmap.Config.ARGB_8888, Bitmap.Config.RGBA_F16)
        } else {
            arrayOf(Bitmap.Config.ARGB_8888)
        }
    }

    /**
     * Return a unique key for this transformation.
     *
     * The key should contain any params that are part of this transformation (e.g. size, scale, color, radius, etc.).
     */
    fun key(): String

    /**
     * Apply the transformation to [input].
     *
     * For optimal performance, do not use [Bitmap.createBitmap] inside this method. Instead, use the provided
     * [BitmapPool] to get new [Bitmap]s. Also, you should return every bitmap except the output bitmap to [pool]
     * so that they can be reused.
     *
     * @param pool A [BitmapPool] which can be used to request [Bitmap] instances.
     * @param input The input [Bitmap] to transform. Its config will always be one of [VALID_CONFIGS].
     * @param size The size of the image request.
     *
     * @see BitmapPool.get
     * @see BitmapPool.put
     */
    suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size): Bitmap
}
