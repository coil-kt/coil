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

/**
 * Wraps an [Image] so it can be used as a [Painter].
 */
class ImagePainter(
    val image: Image,
) : Painter() {
    override val intrinsicSize: Size
        get() = Size(intrinsicWidth, intrinsicHeight)

    private val intrinsicWidth: Float
        get() = image.width.let { if (it > 0) it.toFloat() else Float.NaN }

    private val intrinsicHeight: Float
        get() = image.height.let { if (it > 0) it.toFloat() else Float.NaN }

    override fun DrawScope.onDraw() {
        scale(
            scaleX = image.width.let { if (it > 0) size.width / it else 1f },
            scaleY = image.height.let { if (it > 0) size.height / it else 1f },
            pivot = Offset.Zero,
        ) {
            image.draw(drawContext.canvas.nativeCanvas)
        }
    }
}

/** Convert this [Image] into a [Painter] using Compose primitives if possible. */
expect fun Image.asPainter(
    context: PlatformContext,
    filterQuality: FilterQuality = DefaultFilterQuality,
): Painter

internal expect val Canvas.nativeCanvas: coil3.Canvas
