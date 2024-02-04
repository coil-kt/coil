@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package coil3.test

import coil3.Image
import coil3.annotation.Data
import coil3.annotation.ExperimentalCoilApi
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorInfo
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.impl.use

@ExperimentalCoilApi
@Data
actual class FakeImage actual constructor(
    override val width: Int,
    override val height: Int,
    override val size: Long,
    override val shareable: Boolean,
    actual val color: Int,
) : Image {
    override fun toBitmap(): Bitmap {
        val bitmap = Bitmap()
        bitmap.setImageInfo(ImageInfo(colorInfo, width, height))
        Canvas(bitmap).use { canvas ->
            val paint = Paint()
            paint.color = color
            canvas.drawPaint(paint)
        }
        return bitmap
    }
}

private val colorInfo = ColorInfo(
    colorType = ColorType.RGBA_8888,
    alphaType = ColorAlphaType.PREMUL,
    colorSpace = null,
)
