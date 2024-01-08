package coil3

import coil3.annotation.Data
import coil3.annotation.ExperimentalCoilApi
import kotlin.jvm.JvmOverloads
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas

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

    fun Canvas.onDraw()

    fun asBitmap(): Bitmap {
        val bitmap = Bitmap()
        bitmap.allocN32Pixels(width, height)
        Canvas(bitmap).onDraw()
        return bitmap
    }
}
@ExperimentalCoilApi
@Data
class BitmapImage internal constructor(
    bitmap: Bitmap,
    override val shareable: Boolean,
) : Image {
    private val image =
        try {
            org.jetbrains.skia.Image.makeFromBitmap(bitmap)
        } finally {
            bitmap.close()
        }

    override val size: Long
        get() {
            var size = image.imageInfo.computeMinByteSize().toLong()
            if (size <= 0L) {
                // Estimate 4 bytes per pixel.
                size = 4L * image.width * image.height
            }
            return size
        }

    override val width: Int
        get() = image.width

    override val height: Int
        get() = image.height

    override fun Canvas.onDraw() {
        drawImage(
            image = image,
            top = 0f,
            left = 0f,
        )
    }
}
