package coil.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import androidx.annotation.WorkerThread
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.component3
import androidx.core.graphics.component4
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import coil.bitmappool.BitmapPool
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Scale
import coil.size.Size
import coil.util.normalize
import coil.util.toDrawable
import kotlin.math.roundToInt

internal class DrawableDecoderService(
    private val context: Context,
    private val bitmapPool: BitmapPool
) {

    companion object {
        private const val DEFAULT_SIZE = 512
    }

    @WorkerThread
    fun convertIfNecessary(
        drawable: Drawable,
        size: Size,
        config: Bitmap.Config
    ): Drawable {
        return if (shouldConvertToBitmap(drawable)) {
            convert(drawable, size, config).toDrawable(context)
        } else {
            drawable
        }
    }

    /** Convert the provided [Drawable] into a [Bitmap]. */
    @WorkerThread
    fun convert(
        drawable: Drawable,
        size: Size,
        config: Bitmap.Config
    ): Bitmap {
        // Treat HARDWARE configs as ARGB_8888.
        val safeConfig = config.normalize()

        // Fast path to return the Bitmap.
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            if (bitmap.config.normalize() == safeConfig) {
                return bitmap
            }
        }

        val width: Int
        val height: Int
        val unsafeIntrinsicWidth = drawable.intrinsicWidth
        val unsafeIntrinsicHeight = drawable.intrinsicHeight
        val intrinsicWidth = if (unsafeIntrinsicWidth > 0) unsafeIntrinsicWidth else DEFAULT_SIZE
        val intrinsicHeight = if (unsafeIntrinsicHeight > 0) unsafeIntrinsicHeight else DEFAULT_SIZE
        when (size) {
            is OriginalSize -> {
                width = intrinsicWidth
                height = intrinsicHeight
            }
            is PixelSize -> {
                val multiplier = DecodeUtils.computeSizeMultiplier(
                    srcWidth = intrinsicWidth.toFloat(),
                    srcHeight = intrinsicHeight.toFloat(),
                    destWidth = size.width.toFloat(),
                    destHeight = size.height.toFloat(),
                    scale = Scale.FIT
                )
                width = (multiplier * intrinsicWidth).roundToInt()
                height = (multiplier * intrinsicHeight).roundToInt()
            }
        }

        val (oldLeft, oldTop, oldRight, oldBottom) = drawable.bounds

        // Draw the drawable on the bitmap.
        val bitmap = bitmapPool.get(width, height, safeConfig)
        drawable.apply {
            setBounds(0, 0, width, height)
            draw(Canvas(bitmap))
            setBounds(oldLeft, oldTop, oldRight, oldBottom)
        }

        return bitmap
    }

    private fun shouldConvertToBitmap(drawable: Drawable): Boolean {
        return (drawable is VectorDrawableCompat) || (SDK_INT > LOLLIPOP && drawable is VectorDrawable)
    }
}
