@file:JvmName("AsyncImagePainterSingletonKt")
@file:Suppress("DEPRECATION_ERROR", "unused", "UNUSED_PARAMETER")

package coil.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImagePainter.ExecuteCallback
import coil.request.ImageRequest

/**
 * Return an [AsyncImagePainter] that executes an [ImageRequest] asynchronously and
 * renders the result.
 *
 * @param model Either an [ImageRequest] or the [ImageRequest.data] value.
 */
@Composable
fun rememberAsyncImagePainter(model: Any?): AsyncImagePainter =
    rememberAsyncImagePainter(model, LocalImageLoader.current)

@Deprecated(
    message = "ImagePainter has been renamed to AsyncImagePainter.",
    replaceWith = ReplaceWith(
        expression = "rememberAsyncImagePainter(" +
            "ImageRequest.Builder(LocalContext.current).data(data).apply(builder).build())",
        imports = ["coil.compose.rememberAsyncImagePainter", "coil.request.ImageRequest"]
    ),
    level = DeprecationLevel.ERROR
)
@Composable
inline fun rememberImagePainter(
    data: Any?,
    onExecute: ExecuteCallback = ExecuteCallback.Default,
    builder: ImageRequest.Builder.() -> Unit = {},
): AsyncImagePainter = rememberAsyncImagePainter(
    model = ImageRequest.Builder(LocalContext.current)
        .data(data)
        .apply(builder)
        .build()
)

@Deprecated(
    message = "ImagePainter has been renamed to AsyncImagePainter.",
    replaceWith = ReplaceWith(
        expression = "rememberAsyncImagePainter(request)",
        imports = ["coil.compose.rememberAsyncImagePainter"]
    ),
    level = DeprecationLevel.ERROR
)
@Composable
fun rememberImagePainter(
    request: ImageRequest,
    onExecute: ExecuteCallback = ExecuteCallback.Default,
): AsyncImagePainter = rememberAsyncImagePainter(request)
