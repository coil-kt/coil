@file:Suppress("NOTHING_TO_INLINE", "unused", "UNUSED_PARAMETER")

package coil.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImagePainter.State
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
        expression = "rememberAsyncImagePainter(data, imageLoader)",
        imports = ["coil.compose.rememberAsyncImagePainter"]
    )
)
@Composable
inline fun rememberImagePainter(
    data: Any?,
    imageLoader: ImageLoader,
) = rememberAsyncImagePainter(data, imageLoader)

@Deprecated(
    message = "ImagePainter has been renamed to AsyncImagePainter.",
    replaceWith = ReplaceWith(
        expression = "rememberAsyncImagePainter(data, imageLoader)",
        imports = ["coil.compose.rememberAsyncImagePainter"]
    ),
    level = DeprecationLevel.ERROR // ExecuteCallback is no longer supported.
)
@Composable
inline fun rememberImagePainter(
    data: Any?,
    imageLoader: ImageLoader,
    onExecute: ExecuteCallback
) = rememberAsyncImagePainter(data, imageLoader)

@Deprecated(
    message = "ImagePainter has been renamed to AsyncImagePainter.",
    replaceWith = ReplaceWith(
        expression = "rememberAsyncImagePainter(ImageRequest.Builder(LocalContext.current)" +
            ".data(data).apply(builder).build(), imageLoader)",
        imports = [
            "androidx.compose.ui.platform.LocalContext",
            "coil.compose.rememberAsyncImagePainter",
            "coil.request.ImageRequest",
        ]
    )
)
@Composable
inline fun rememberImagePainter(
    data: Any?,
    imageLoader: ImageLoader,
    builder: ImageRequest.Builder.() -> Unit,
) = rememberAsyncImagePainter(
    model = ImageRequest.Builder(LocalContext.current).data(data).apply(builder).build(),
    imageLoader = imageLoader
)

@Deprecated(
    message = "ImagePainter has been renamed to AsyncImagePainter.",
    replaceWith = ReplaceWith(
        expression = "rememberAsyncImagePainter(ImageRequest.Builder(LocalContext.current)" +
            ".data(data).apply(builder).build(), imageLoader)",
        imports = [
            "androidx.compose.ui.platform.LocalContext",
            "coil.compose.rememberAsyncImagePainter",
            "coil.request.ImageRequest",
        ]
    ),
    level = DeprecationLevel.ERROR // ExecuteCallback is no longer supported.
)
@Composable
inline fun rememberImagePainter(
    data: Any?,
    imageLoader: ImageLoader,
    onExecute: ExecuteCallback,
    builder: ImageRequest.Builder.() -> Unit,
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
inline fun rememberImagePainter(
    request: ImageRequest,
    imageLoader: ImageLoader,
) = rememberAsyncImagePainter(request, imageLoader)

@Deprecated(
    message = "ImagePainter has been renamed to AsyncImagePainter.",
    replaceWith = ReplaceWith(
        expression = "rememberAsyncImagePainter(request, imageLoader)",
        imports = ["coil.compose.rememberAsyncImagePainter"]
    ),
    level = DeprecationLevel.ERROR // ExecuteCallback is no longer supported.
)
@Composable
inline fun rememberImagePainter(
    request: ImageRequest,
    imageLoader: ImageLoader,
    onExecute: ExecuteCallback,
) = rememberAsyncImagePainter(request, imageLoader)

private typealias ExecuteCallback = (Snapshot, Snapshot) -> Unit

private typealias Snapshot = Triple<State, ImageRequest, Size>
