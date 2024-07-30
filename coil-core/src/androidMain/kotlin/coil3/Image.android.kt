package coil3

import android.content.res.Resources
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import coil3.annotation.Poko
import coil3.util.allocationByteCountCompat
import coil3.util.height
import coil3.util.width

actual typealias Bitmap = android.graphics.Bitmap

actual typealias Canvas = android.graphics.Canvas

@JvmOverloads
actual fun Bitmap.asImage(shareable: Boolean): BitmapImage {
    return BitmapImage(this, shareable)
}

@JvmOverloads
actual fun Image.toBitmap(
    width: Int,
    height: Int,
): Bitmap = toBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)

fun Image.toBitmap(
    width: Int,
    height: Int,
    config: android.graphics.Bitmap.Config,
): Bitmap {
    if (this is BitmapImage &&
        bitmap.width == width &&
        bitmap.height == height &&
        bitmap.config == config
    ) {
        return bitmap
    }

    return createBitmap(width, height, config).applyCanvas(::draw)
}

/**
 * An [Image] backed by an Android [Bitmap].
 */
@Poko
actual class BitmapImage internal constructor(
    actual val bitmap: Bitmap,
    actual override val shareable: Boolean,
) : Image {

    actual override val size: Long
        get() = bitmap.allocationByteCountCompat.toLong()

    actual override val width: Int
        get() = bitmap.width

    actual override val height: Int
        get() = bitmap.height

    actual override fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, 0f, 0f, null)
    }
}

fun Drawable.asImage(): Image {
    return if (this is BitmapDrawable) {
        bitmap.asImage()
    } else {
        DrawableImage(this, false)
    }
}

fun Drawable.asImage(shareable: Boolean): Image {
    return if (this is BitmapDrawable) {
        bitmap.asImage(shareable)
    } else {
        DrawableImage(this, shareable)
    }
}

fun Image.asDrawable(resources: Resources): Drawable {
    return when (this) {
        is DrawableImage -> drawable
        is BitmapImage -> BitmapDrawable(resources, bitmap)
        else -> ImageDrawable(this)
    }
}

/**
 * An [Image] backed by an Android [Drawable].
 */
@Poko
class DrawableImage internal constructor(
    val drawable: Drawable,
    override val shareable: Boolean,
) : Image {

    override val size: Long
        get() {
            val size = if (drawable is SizeProvider) {
                drawable.size
            } else {
                // Estimate 4 bytes per pixel.
                4L * drawable.width * drawable.height
            }
            return size.coerceAtLeast(0)
        }

    override val width: Int
        get() = drawable.width

    override val height: Int
        get() = drawable.height

    override fun draw(canvas: Canvas) {
        drawable.draw(canvas)
    }

    /**
     * Implement this on your [Drawable] implementation to provide a custom [Image.size].
     */
    interface SizeProvider {
        val size: Long
    }
}

/**
 * A [Drawable] backed by a generic [Image].
 */
class ImageDrawable internal constructor(
    val image: Image,
) : Drawable() {

    override fun draw(canvas: Canvas) {
        image.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        // Unsupported
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        // Unsupported
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.UNKNOWN
}
