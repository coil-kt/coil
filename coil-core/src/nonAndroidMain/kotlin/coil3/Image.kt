package coil3

import coil3.annotation.Data
import coil3.annotation.ExperimentalCoilApi
import kotlin.jvm.JvmOverloads
import org.jetbrains.skia.Bitmap

@ExperimentalCoilApi
@JvmOverloads
fun Bitmap.asCoilImage(
    shareable: Boolean = isImmutable,
): Image = BitmapImage(this, shareable)

@ExperimentalCoilApi
actual interface Image {
    actual val size: Int
    actual val width: Int
    actual val height: Int
    actual val shareable: Boolean

    fun toBitmap(): Bitmap
}
@ExperimentalCoilApi
@Data
class BitmapImage internal constructor(
    val bitmap: Bitmap,
    override val shareable: Boolean,
) : Image {
    override val size: Int
        get() {
            var size = bitmap.imageInfo.computeMinByteSize()
            if (size <= 0) {
                // Estimate 4 bytes per pixel.
                size = 4 * bitmap.width * bitmap.height
            }
            return size
        }

    override val width: Int
        get() = bitmap.width

    override val height: Int
        get() = bitmap.height

    override fun toBitmap(): Bitmap = bitmap
}
