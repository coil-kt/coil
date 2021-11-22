@file:Suppress("NOTHING_TO_INLINE", "unused", "UNUSED_PARAMETER")

package coil.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImagePainter.State
import coil.request.ImageRequest

@Deprecated(
    message = "ImagePainter has been renamed to AsyncImagePainter.",
    replaceWith = ReplaceWith(
        expression = "rememberAsyncImagePainter(data)",
        imports = ["coil.compose.rememberAsyncImagePainter"]
    )
)
@Composable
inline fun rememberImagePainter(
    data: Any?,
) = rememberAsyncImagePainter(data)

@Deprecated(
    message = "ImagePainter has been renamed to AsyncImagePainter.",
    replaceWith = ReplaceWith(
        expression = "rememberAsyncImagePainter(data)",
        imports = ["coil.compose.rememberAsyncImagePainter"]
    ),
    level = DeprecationLevel.ERROR // ExecuteCallback is no longer supported.
)
@Composable
inline fun rememberImagePainter(
    data: Any?,
    onExecute: ExecuteCallback,
) = rememberAsyncImagePainter(data)

@Deprecated(
    message = "ImagePainter has been renamed to AsyncImagePainter.",
    replaceWith = ReplaceWith(
        expression = "rememberAsyncImagePainter(" +
            "ImageRequest.Builder(LocalContext.current).data(data).apply(builder).build())",
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
    builder: ImageRequest.Builder.() -> Unit,
) = rememberAsyncImagePainter(
    model = ImageRequest.Builder(LocalContext.current).data(data).apply(builder).build()
)

@Deprecated(
    message = "ImagePainter has been renamed to AsyncImagePainter.",
    replaceWith = ReplaceWith(
        expression = "rememberAsyncImagePainter(" +
            "ImageRequest.Builder(LocalContext.current).data(data).apply(builder).build())",
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
    onExecute: ExecuteCallback,
    builder: ImageRequest.Builder.() -> Unit,
) = rememberAsyncImagePainter(
    model = ImageRequest.Builder(LocalContext.current).data(data).apply(builder).build()
)

@Deprecated(
    message = "ImagePainter has been renamed to AsyncImagePainter.",
    replaceWith = ReplaceWith(
        expression = "rememberAsyncImagePainter(request)",
        imports = ["coil.compose.rememberAsyncImagePainter"]
    )
)
@Composable
inline fun rememberImagePainter(
    request: ImageRequest,
) = rememberAsyncImagePainter(request)

@Deprecated(
    message = "ImagePainter has been renamed to AsyncImagePainter.",
    replaceWith = ReplaceWith(
        expression = "rememberAsyncImagePainter(request)",
        imports = ["coil.compose.rememberAsyncImagePainter"]
    ),
    level = DeprecationLevel.ERROR // ExecuteCallback is no longer supported.
)
@Composable
inline fun rememberImagePainter(
    request: ImageRequest,
    onExecute: ExecuteCallback,
) = rememberAsyncImagePainter(request)

private typealias ExecuteCallback = (Snapshot, Snapshot) -> Boolean

private typealias Snapshot = Triple<State, ImageRequest, Size>
