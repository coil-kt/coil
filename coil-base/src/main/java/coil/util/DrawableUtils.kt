package coil.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.WorkerThread
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.component3
import androidx.core.graphics.component4
import androidx.core.graphics.createBitmap
import coil.decode.DecodeUtils
import coil.size.OriginalSize
import coil.size.Scale
import coil.size.Size

internal object DrawableUtils {

    private const val DEFAULT_SIZE = 512

    /**
     * Convert the provided [Drawable] into a [Bitmap].
     *
     * @param drawable The drawable to convert.
     * @param config The requested config for the bitmap.
     * @param size The requested size for the bitmap.
     * @param scale The requested scale for the bitmap.
     * @param allowInexactSize Allow returning a bitmap that is smaller than [size].
     */
    @WorkerThread
    fun convertToBitmap(
        drawable: Drawable,
        config: Bitmap.Config,
        size: Size,
        scale: Scale,
        allowInexactSize: Boolean
    ): Bitmap {
        // Fast path: return the underlying bitmap.
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            if (isConfigValid(bitmap, config) && isSizeValid(allowInexactSize, size, bitmap, scale)) {
                return bitmap
            }
        }

        // Slow path: draw the drawable on a new bitmap.
        val safeDrawable = drawable.mutate()
        val srcWidth = safeDrawable.width.let { if (it > 0) it else DEFAULT_SIZE }
        val srcHeight = safeDrawable.height.let { if (it > 0) it else DEFAULT_SIZE }
        val (width, height) = DecodeUtils.computePixelSize(srcWidth, srcHeight, size, scale)

        val bitmap = createBitmap(width, height, config.toSoftware())
        safeDrawable.apply {
            val (oldLeft, oldTop, oldRight, oldBottom) = safeDrawable.bounds
            setBounds(0, 0, width, height)
            draw(Canvas(bitmap))
            setBounds(oldLeft, oldTop, oldRight, oldBottom)
        }

        return bitmap
    }

    private fun isConfigValid(bitmap: Bitmap, config: Bitmap.Config): Boolean {
        return bitmap.config == config.toSoftware()
    }

    private fun isSizeValid(allowInexactSize: Boolean, size: Size, bitmap: Bitmap, scale: Scale): Boolean {
        return allowInexactSize || size is OriginalSize ||
            size == DecodeUtils.computePixelSize(bitmap.width, bitmap.height, size, scale)
    }
}
