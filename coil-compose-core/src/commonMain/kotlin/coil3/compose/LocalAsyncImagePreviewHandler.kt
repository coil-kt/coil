package coil3.compose

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalInspectionMode
import coil3.Image
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePainter.State.Error
import coil3.compose.AsyncImagePainter.State.Loading
import coil3.compose.AsyncImagePainter.State.Success
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlin.jvm.JvmField

@ExperimentalCoilApi
val LocalAsyncImagePreviewHandler = staticCompositionLocalOf {
    AsyncImagePreviewHandler.Default
}

/**
 * Controls what [AsyncImage], [SubcomposeAsyncImage], and [AsyncImagePainter] render when
 * [LocalInspectionMode] is true.
 */
@ExperimentalCoilApi
@Stable
fun interface AsyncImagePreviewHandler {

    suspend fun handle(
        imageLoader: ImageLoader,
        request: ImageRequest,
    ): AsyncImagePainter.State

    companion object {
        @JvmField val Default = AsyncImagePreviewHandler { imageLoader, request ->
            when (val result = imageLoader.execute(request)) {
                is SuccessResult -> Success(result.image.asPainter(request.context), result)
                is ErrorResult -> Error(result.image?.asPainter(request.context), result)
            }
        }
    }
}

/**
 * Convenience function that creates an [AsyncImagePreviewHandler] that returns an [Image].
 */
@ExperimentalCoilApi
inline fun AsyncImagePreviewHandler(
    crossinline image: suspend (request: ImageRequest) -> Image?,
) = AsyncImagePreviewHandler { _, request ->
    Loading(image(request)?.asPainter(request.context))
}
