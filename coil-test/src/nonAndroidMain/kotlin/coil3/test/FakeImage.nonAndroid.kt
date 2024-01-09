@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package coil3.test

import coil3.Image
import coil3.annotation.Data
import coil3.annotation.ExperimentalCoilApi
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorInfo
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Paint

@ExperimentalCoilApi
@Data
actual class FakeImage actual constructor(
    override val width: Int,
    override val height: Int,
    override val size: Long,
    override val shareable: Boolean,
    actual val color: Int,
) : Image {
    override fun Canvas.onDraw() {
        val paint = Paint()
        paint.color = color
        drawPaint(paint)
    }
}

private val colorInfo = ColorInfo(
    colorType = ColorType.RGBA_8888,
    alphaType = ColorAlphaType.PREMUL,
    colorSpace = null,
)
