package coil3

import coil3.annotation.Data
import coil3.annotation.ExperimentalCoilApi
import kotlin.jvm.JvmOverloads
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.svg.SVGDOM

@ExperimentalCoilApi
@JvmOverloads
fun Bitmap.asCoilImage(
    shareable: Boolean = isImmutable,
): Image = BitmapImage(this, shareable)

@ExperimentalCoilApi
fun SVGDOM.asCoilImage(
    width: Int,
    height: Int,
): Image = SvgImage(
    svg = this,
    width = width,
    height = height,
)

@ExperimentalCoilApi
actual interface Image {
    actual val size: Long
    actual val width: Int
    actual val height: Int
    actual val shareable: Boolean

    fun asPainter(): CoilPainter

    fun asBitmap(): Bitmap = when (val painter = asPainter()) {
        is CoilPainter.BitmapPainter -> painter.asBitmap()
        is CoilPainter.VectorPainter -> {
            val bitmap = Bitmap()
            bitmap.allocN32Pixels(width, height)
            with(painter) {
                Canvas(bitmap).onDraw()
            }
            bitmap
        }
    }
}

@ExperimentalCoilApi
@Data
class BitmapImage internal constructor(
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

    override fun asPainter(): CoilPainter = CoilPainter.BitmapPainter {
        bitmap
    }
}

@ExperimentalCoilApi
@Data
class SvgImage internal constructor(
    val svg: SVGDOM,
    override val width: Int,
    override val height: Int,
) : Image {
    override val size: Long
        get() = 4L * width * height

    override val shareable: Boolean = true

    override fun asPainter(): CoilPainter = CoilPainter.VectorPainter {
        svg.render(this)
    }
}
