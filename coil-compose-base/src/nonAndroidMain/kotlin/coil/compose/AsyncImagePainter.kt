package coil.compose

import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil.Image
import coil.bitmap
import coil.request.SuccessResult
import coil.request.crossfadeMillis

internal actual fun Image.toPainter(
    filterQuality: FilterQuality,
): Painter = BitmapPainter(
    image = bitmap.asComposeImageBitmap(),
    filterQuality = filterQuality,
)

internal actual fun maybeNewCrossfadePainter(
    previous: AsyncImagePainter.State,
    current: AsyncImagePainter.State,
    contentScale: ContentScale,
): CrossfadePainter? {
    // We can only invoke the transition factory if the state is success or error.
    val result = when (current) {
        is AsyncImagePainter.State.Success -> current.result
        is AsyncImagePainter.State.Error -> current.result
        else -> return null
    }

    // Wrap the painter in a CrossfadePainter if it has crossfadeMillis set.
    val crossfadeMillis = result.request.crossfadeMillis
    if (crossfadeMillis > 0) {
        return CrossfadePainter(
            start = previous.painter.takeIf { previous is AsyncImagePainter.State.Loading },
            end = current.painter,
            contentScale = contentScale,
            durationMillis = crossfadeMillis,
            fadeStart = result !is SuccessResult || !result.isPlaceholderCached,
            preferExactIntrinsicSize = false,
        )
    } else {
        return null
    }
}
