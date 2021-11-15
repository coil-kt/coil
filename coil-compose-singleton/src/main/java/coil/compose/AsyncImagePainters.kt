@file:Suppress("unused")

package coil.compose

import androidx.compose.runtime.Composable
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
