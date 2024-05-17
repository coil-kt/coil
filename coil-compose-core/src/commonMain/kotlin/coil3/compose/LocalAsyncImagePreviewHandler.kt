package coil3.compose

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalInspectionMode
import coil3.Image
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.request.ImageRequest
import kotlin.jvm.JvmField

@ExperimentalCoilApi
val LocalAsyncImagePreviewHandler = staticCompositionLocalOf { AsyncImagePreviewHandler.Default }

/**
 * Controls what [AsyncImage], [SubcomposeAsyncImage], and [AsyncImagePainter] renders when
 * [LocalInspectionMode] is true.
 */
@ExperimentalCoilApi
fun interface AsyncImagePreviewHandler {
    fun handle(
        imageLoader: ImageLoader,
        request: ImageRequest,
        toPainter: Image.() -> Painter,
    ): AsyncImagePainter.State

    companion object {
        @JvmField val Default = AsyncImagePreviewHandler { _, request, toPainter ->
            AsyncImagePainter.State.Loading(request.placeholder()?.toPainter())
        }
    }
}
