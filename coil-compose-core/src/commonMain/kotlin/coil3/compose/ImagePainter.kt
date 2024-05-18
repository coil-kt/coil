package coil3.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.painter.Painter
import coil3.Image
import coil3.annotation.ExperimentalCoilApi

/**
 * Wraps an [Image] so it can be used as a [Painter].
 */
@ExperimentalCoilApi
class ImagePainter(
    private val image: Image,
) : Painter() {

    override val intrinsicSize: Size
        get() = Size(
            width = image.width.let { if (it == -1) Float.NaN else it.toFloat() },
            height = image.height.let { if (it == -1) Float.NaN else it.toFloat() },
        )

    override fun DrawScope.onDraw() {
        scale(
            scaleX = size.width / intrinsicSize.width,
            scaleY = size.height / intrinsicSize.height,
            pivot = Offset.Zero,
        ) {
            image.draw(drawContext.canvas.nativeCanvas)
        }
    }
}

internal expect val Canvas.nativeCanvas: coil3.Canvas
