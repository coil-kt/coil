@file:Suppress("unused")

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
 * @param onExecute Called immediately before the [AsyncImagePainter] launches an image request.
 *  Return 'true' to proceed with the request. Return 'false' to skip executing the request.
 */
@Composable
fun rememberAsyncImagePainter(
    model: Any?,
    onExecute: ExecuteCallback = ExecuteCallback.Lazy,
): AsyncImagePainter = rememberAsyncImagePainter(model, LocalImageLoader.current, onExecute)

/******************** DEPRECATED ********************/

/**
 * IntelliJ IDEA's [ReplaceWith] doesn't work well with lambda arguments so call sites using this
 * function should be replaced manually.
 *
 * Call sites that do not use the `builder` argument can be replaced like so:
 *
 * ```
 * rememberImagePainter(data, onExecute)
 * ```
 *
 * Call sites that use the `builder` argument should be converted to create an [ImageRequest]:
 *
 * ```
 * rememberImagePainter(
 *     model = ImageRequest.Builder(LocalContext.current)
 *         .data(data)
 *         .apply(builder)
 *         .build(),
 *     onExecute = onExecute
 * )
 * ```
 */
@Deprecated("ImagePainter has been renamed to AsyncImagePainter.")
@Composable
inline fun rememberImagePainter(
    data: Any?,
    onExecute: ExecuteCallback = ExecuteCallback.Lazy,
    builder: ImageRequest.Builder.() -> Unit = {},
): AsyncImagePainter = rememberAsyncImagePainter(
    model = ImageRequest.Builder(LocalContext.current).data(data).apply(builder).build(),
    imageLoader = LocalImageLoader.current,
    onExecute = onExecute
)

@Deprecated(
    message = "ImagePainter has been renamed to AsyncImagePainter.",
    replaceWith = ReplaceWith(
        expression = "rememberAsyncImagePainter(request, onExecute)",
        imports = ["coil.compose.rememberAsyncImagePainter"]
    )
)
@Composable
fun rememberImagePainter(
    request: ImageRequest,
    onExecute: ExecuteCallback = ExecuteCallback.Lazy,
): AsyncImagePainter = rememberAsyncImagePainter(request, LocalImageLoader.current, onExecute)
