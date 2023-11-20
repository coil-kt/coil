package coil

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import coil.util.allocationByteCountCompat
import coil.util.height
import coil.util.width

private class WrappedDrawableImage(
    val drawable: Drawable,
    override val shareable: Boolean,
) : Image {

    override val size: Long
        get() {
            if (drawable is BitmapDrawable) {
                return drawable.bitmap.allocationByteCountCompat.toLong()
            } else {
                // Estimate 4 bytes per pixel.
                return 4L * drawable.width * drawable.height
            }
        }

    override val width: Int
        get() = drawable.width

    override val height: Int
        get() = drawable.height
}

@JvmOverloads
fun Drawable.asCoilImage(
    shareable: Boolean = this is BitmapDrawable
): Image = WrappedDrawableImage(this, shareable)

val Image.drawable: Drawable
    get() = (this as WrappedDrawableImage).drawable
