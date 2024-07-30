package coil3.compose

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import coil3.BitmapImage
import coil3.Image
import coil3.PlatformContext
import coil3.toBitmap

actual fun Image.asPainter(
    context: PlatformContext,
    filterQuality: FilterQuality,
): Painter = when (this) {
    is BitmapImage -> BitmapPainter(
        image = toBitmap().asComposeImageBitmap(),
        filterQuality = filterQuality,
    )
    else -> ImagePainter(this)
}

internal actual val Canvas.nativeCanvas get() = nativeCanvas
