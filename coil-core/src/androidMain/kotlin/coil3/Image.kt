package coil3

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import coil3.annotation.Data
import coil3.annotation.ExperimentalCoilApi
import coil3.util.allocationByteCountCompat
import coil3.util.height
import coil3.util.isImmutable
import coil3.util.width

@ExperimentalCoilApi
fun Drawable.asCoilImage(): Image {
    return if (this is BitmapDrawable) {
        bitmap.asCoilImage()
    } else {
        DrawableImage(this, false)
    }
}

@ExperimentalCoilApi
fun Drawable.asCoilImage(shareable: Boolean): Image {
    return if (this is BitmapDrawable) {
        bitmap.asCoilImage(shareable)
    } else {
        DrawableImage(this, shareable)
    }
}

@ExperimentalCoilApi
@JvmOverloads
fun Bitmap.asCoilImage(
    shareable: Boolean = isImmutable,
): Image = BitmapImage(this, shareable)

@ExperimentalCoilApi
actual interface Image {
    actual val size: Long
    actual val width: Int
    actual val height: Int
    actual val shareable: Boolean

    fun asDrawable(resources: Resources): Drawable
}

@ExperimentalCoilApi
@Data
class DrawableImage internal constructor(
    val drawable: Drawable,
    override val shareable: Boolean,
) : Image {

    override val size: Long
        get() = if (drawable is SizeProvider) {
            drawable.size
        } else {
            // Estimate 4 bytes per pixel.
            4L * drawable.width * drawable.height
        }

    override val width: Int
        get() = drawable.width

    override val height: Int
        get() = drawable.height

    override fun asDrawable(resources: Resources): Drawable {
        return drawable
    }

    interface SizeProvider {
        val size: Long
    }
}

@ExperimentalCoilApi
@Data
class BitmapImage internal constructor(
    val bitmap: Bitmap,
    override val shareable: Boolean,
) : Image {

    override val size: Long
        get() = bitmap.allocationByteCountCompat.toLong()

    override val width: Int
        get() = bitmap.width

    override val height: Int
        get() = bitmap.height

    override fun asDrawable(resources: Resources): Drawable {
        return BitmapDrawable(resources, bitmap)
    }
}
