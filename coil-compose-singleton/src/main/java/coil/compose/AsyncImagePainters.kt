@file:Suppress("unused")

package coil.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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

/******************** DEPRECATED ********************/

@Deprecated(
    message = "ImagePainter has been renamed to AsyncImagePainter.",
    replaceWith = ReplaceWith(
        expression = "rememberAsyncImagePainter(" +
            "ImageRequest.Builder(LocalContext.current).data(data).apply(builder).build())",
        imports = ["coil.compose.rememberAsyncImagePainter", "coil.request.ImageRequest"]
    )
)
@Composable
inline fun rememberImagePainter(
    data: Any?,
    builder: ImageRequest.Builder.() -> Unit = {},
) = rememberAsyncImagePainter(
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
    )
)
@Composable
fun rememberImagePainter(request: ImageRequest) = rememberAsyncImagePainter(request)
