package coil3.compose

import androidx.compose.ui.layout.ContentScale
import coil3.compose.internal.CrossfadePainter
import coil3.decode.DataSource
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.crossfadeMillis

internal actual fun validateRequestProperties(request: ImageRequest) {
    require(request.target == null) { "request.target must be null." }
}

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

    // Only animate successful requests.
    if (result !is SuccessResult) {
        return null
    }

    // Don't animate if the request was fulfilled by the memory cache.
    if (result.dataSource == DataSource.MEMORY_CACHE) {
        return null
    }

    // Wrap the painter in a CrossfadePainter if it has crossfadeMillis set.
    val crossfadeMillis = result.request.crossfadeMillis
    if (crossfadeMillis > 0) {
        return CrossfadePainter(
            start = previous.painter.takeIf { previous is AsyncImagePainter.State.Loading },
            end = current.painter,
            contentScale = contentScale,
            durationMillis = crossfadeMillis,
            fadeStart = !result.isPlaceholderCached,
            preferExactIntrinsicSize = false,
        )
    } else {
        return null
    }
}
