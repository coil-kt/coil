package coil.transform

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import androidx.collection.arraySetOf
import coil.bitmappool.BitmapPool
import coil.decode.DecodeResult
import coil.fetch.DrawableResult
import coil.request.RequestBuilder

/**
 * An interface for making transformations to an image's pixel data.
 *
 * NOTE: Transformations are only applied if [DrawableResult.drawable] or [DecodeResult.drawable] is a [BitmapDrawable].
 *
 * @see RequestBuilder.transformations
 */
interface Transformation {

    companion object {
        /**
         * A whitelist of valid bitmap configs for the input and output bitmaps of [transform].
         */
        internal val VALID_CONFIGS = if (SDK_INT >= O) {
            arraySetOf(Bitmap.Config.ARGB_8888, Bitmap.Config.RGBA_F16)
        } else {
            arraySetOf(Bitmap.Config.ARGB_8888)
        }
    }

    /**
     * Return a unique key for this transformation.
     *
     * The key should contain any params that are part of this transformation (e.g. size, scale, color, radius, etc.).
     */
    fun key(): String

    /**
     * Apply the transformation to [Bitmap].
     *
     * For optimal performance, do not use [Bitmap.createBitmap] inside this method. Instead, use the provided
     * [BitmapPool] to get new [Bitmap]s. Also, you should return every Bitmap except the output [Bitmap] to the
     * pool so that they can be reused.
     *
     * @see BitmapPool.get
     * @see BitmapPool.put
     */
    suspend fun transform(pool: BitmapPool, input: Bitmap): Bitmap
}
