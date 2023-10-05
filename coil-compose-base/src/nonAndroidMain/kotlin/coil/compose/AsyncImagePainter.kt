package coil.compose

import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil.Image

internal actual fun Image.toPainter(
    filterQuality: FilterQuality,
): Painter = TODO()

/** Create and return a [CrossfadePainter] if requested. */
internal actual fun maybeNewCrossfadePainter(
    previous: AsyncImagePainter.State,
    current: AsyncImagePainter.State,
    contentScale: ContentScale,
): CrossfadePainter? = TODO()
