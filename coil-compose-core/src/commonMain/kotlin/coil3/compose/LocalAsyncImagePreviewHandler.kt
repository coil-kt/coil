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
import kotlin.jvm.JvmName

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
@JvmName("AsyncImagePreviewHandlerNotNull")
inline fun AsyncImagePreviewHandler(
    crossinline image: suspend (request: ImageRequest) -> Image,
) = AsyncImagePreviewHandler { _, request ->
    image(request).let { Success(it.asPainter(request.context), SuccessResult(it, request)) }
}

@Deprecated(
    message = "Migrate to the AsyncImagePreviewHandler constructor that returns a not null Image. " +
        "Alternatively, if you need to return a nullable Image, inline this code into your call site.",
    replaceWith = ReplaceWith(
        expression = "AsyncImagePreviewHandler { _, request -> " +
            "AsyncImagePainter.State.Loading(image(request)?.asPainter(request.context)) }",
        imports = ["coil3.compose.AsyncImagePainter"],
    ),
    level = DeprecationLevel.ERROR,
)
@ExperimentalCoilApi
inline fun AsyncImagePreviewHandler(
    crossinline image: suspend (request: ImageRequest) -> Image?,
) = AsyncImagePreviewHandler { _, request ->
    Loading(image(request)?.asPainter(request.context))
}
