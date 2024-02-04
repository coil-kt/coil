package coil3.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import coil3.DrawableImage

class DrawablePainter(
    private val drawableImage: DrawableImage,
) : Painter() {

    override val intrinsicSize: Size =
        Size(drawableImage.width.toFloat(), drawableImage.height.toFloat())

    override fun DrawScope.onDraw() {
        val scaleX = size.width / intrinsicSize.width
        val scaleY = size.height / intrinsicSize.height

        with(drawableImage) {
            scale(
                scaleX = scaleX,
                scaleY = scaleY,
                pivot = Offset.Zero,
            ) {
                drawContext.canvas.nativeCanvas.onDraw()
            }
        }
    }
}
