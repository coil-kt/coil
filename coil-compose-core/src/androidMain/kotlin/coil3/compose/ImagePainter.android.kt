package coil3.compose

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import coil3.BitmapImage
import coil3.DrawableImage
import coil3.Image
import coil3.PlatformContext
import coil3.asDrawable
import com.google.accompanist.drawablepainter.DrawablePainter

actual fun Image.asPainter(
    context: PlatformContext,
    filterQuality: FilterQuality,
): Painter = when (this) {
    is BitmapImage -> BitmapPainter(
        image = bitmap.asImageBitmap(),
        filterQuality = filterQuality,
    )
    is DrawableImage -> DrawablePainter(
        drawable = asDrawable(context.resources).mutate(),
    )
    else -> ImagePainter(this)
}

internal actual val Canvas.nativeCanvas get() = nativeCanvas
