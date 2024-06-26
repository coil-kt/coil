package coil3.compose

import androidx.compose.foundation.Image
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.painter.Painter
import coil3.Image
import coil3.PlatformContext
import coil3.annotation.ExperimentalCoilApi

/**
 * Wraps an [Image] so it can be used as a [Painter].
 */
@ExperimentalCoilApi
class ImagePainter(
    val image: Image,
) : Painter() {
    override val intrinsicSize: Size
        get() = Size(intrinsicWidth, intrinsicHeight)

    private val intrinsicWidth: Float
        get() = image.width.toDimension()

    private val intrinsicHeight: Float
        get() = image.height.toDimension()

    override fun DrawScope.onDraw() {
        scale(
            scaleX = size.width / intrinsicWidth,
            scaleY = size.height / intrinsicHeight,
            pivot = Offset.Zero,
        ) {
            image.draw(drawContext.canvas.nativeCanvas)
        }
    }

    private fun Int.toDimension(): Float {
        return if (this >= 0) toFloat() else Float.NaN
    }
}

/** Convert this [Image] into a [Painter] using Compose primitives if possible. */
@ExperimentalCoilApi
expect fun Image.asPainter(
    context: PlatformContext,
    filterQuality: FilterQuality = DefaultFilterQuality,
): Painter

internal expect val Canvas.nativeCanvas: coil3.Canvas
