@file:Suppress("unused")

package coil.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.request.ImageRequest

@Deprecated(
    message = "ImagePainter has been renamed to AsyncImagePainter.",
    replaceWith = ReplaceWith(
        expression = "AsyncImagePainter",
        imports = ["coil.compose.AsyncImagePainter"]
    )
)
typealias ImagePainter = AsyncImagePainter

@Deprecated(
    message = "ImagePainter has been renamed to AsyncImagePainter.",
    replaceWith = ReplaceWith(
        expression = "rememberAsyncImagePainter(" +
            "ImageRequest.Builder(LocalContext.current).data(data).apply(builder).build(), " +
            "imageLoader)",
        imports = ["coil.compose.rememberAsyncImagePainter", "coil.request.ImageRequest"]
    )
)
@Composable
inline fun rememberImagePainter(
    data: Any?,
    imageLoader: ImageLoader,
    builder: ImageRequest.Builder.() -> Unit = {},
) = rememberAsyncImagePainter(
    model = ImageRequest.Builder(LocalContext.current).data(data).apply(builder).build(),
    imageLoader = imageLoader
)

@Deprecated(
    message = "ImagePainter has been renamed to AsyncImagePainter.",
    replaceWith = ReplaceWith(
        expression = "rememberAsyncImagePainter(request, imageLoader)",
        imports = ["coil.compose.rememberAsyncImagePainter"]
    )
)
@Composable
fun rememberImagePainter(
    request: ImageRequest,
    imageLoader: ImageLoader,
) = rememberAsyncImagePainter(
    model = request,
    imageLoader = imageLoader
)
