@file:Suppress("unused")

package coil.compose

import androidx.compose.runtime.Composable
import coil.request.ImageRequest

/**
 * Return an [ImagePainter] that will execute an [ImageRequest] using [LocalImageLoader].
 *
 * @param data The [ImageRequest.data] to load.
 * @param onExecute Called immediately before the [ImagePainter] launches an image request.
 *  Return 'true' to proceed with the request. Return 'false' to skip executing the request.
 * @param builder An optional lambda to configure the request.
 */
@Composable
inline fun rememberImagePainter(
    data: Any?,
    onExecute: ImagePainter.ExecuteCallback = ImagePainter.ExecuteCallback.Lazy,
    builder: ImageRequest.Builder.() -> Unit = {},
): ImagePainter = rememberImagePainter(data, LocalImageLoader.current, onExecute, builder)

/**
 * Return an [ImagePainter] that will execute the [request] using [LocalImageLoader].
 *
 * @param request The [ImageRequest] to execute.
 * @param onExecute Called immediately before the [ImagePainter] launches an image request.
 *  Return 'true' to proceed with the request. Return 'false' to skip executing the request.
 */
@Composable
fun rememberImagePainter(
    request: ImageRequest,
    onExecute: ImagePainter.ExecuteCallback = ImagePainter.ExecuteCallback.Lazy,
): ImagePainter = rememberImagePainter(request, LocalImageLoader.current, onExecute)
