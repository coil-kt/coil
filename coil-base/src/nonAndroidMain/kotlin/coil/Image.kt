package coil

import coil.annotation.ExperimentalCoilApi
import dev.drewhamilton.poko.Poko
import org.jetbrains.skia.Bitmap

@Poko
private class BitmapImage(
    val bitmap: Bitmap,
    override val shareable: Boolean,
) : Image {

    override val size: Long
        get() {
            var size = bitmap.imageInfo.computeMinByteSize().toLong()
            if (size <= 0L) {
                // Estimate 4 bytes per pixel.
                size = 4L * bitmap.width * bitmap.height
            }
            return size
        }

    override val width: Int
        get() = bitmap.width

    override val height: Int
        get() = bitmap.height
}

@ExperimentalCoilApi
fun Bitmap.asCoilImage(
    shareable: Boolean = isImmutable,
): Image = BitmapImage(this, shareable)

@ExperimentalCoilApi
val Image.bitmap: Bitmap
    get() = (this as BitmapImage).bitmap
