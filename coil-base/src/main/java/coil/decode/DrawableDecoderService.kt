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
import coil.size.Size
import coil.util.HAS_APPCOMPAT_RESOURCES
import coil.util.height
import coil.util.normalize
import coil.util.toDrawable
import coil.util.width

internal class DrawableDecoderService(
    private val context: Context,
    private val bitmapPool: BitmapPool
) {

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

        val (width, height) = when (size) {
            is OriginalSize -> PixelSize(drawable.width, drawable.height)
            is PixelSize -> size
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
        return (HAS_APPCOMPAT_RESOURCES && drawable is VectorDrawableCompat) ||
            (SDK_INT > LOLLIPOP && drawable is VectorDrawable)
    }
}
