@file:Suppress("DEPRECATION", "unused")

package coil.compose

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.graphics.painter.Painter
import coil.compose.AsyncImagePainter.Companion.DefaultTransform
import coil.compose.AsyncImagePainter.State
import coil.request.ImageRequest

/**
 * Return an [AsyncImagePainter] that executes an [ImageRequest] asynchronously and
 * renders the result.
 *
 * This is a lower-level API than [AsyncImage] and may not work as expected in all situations.
 * Notably, it will not finish loading if [AsyncImagePainter.onDraw] is not called, which can occur
 * for composables that don't have a fixed size (e.g. [LazyColumn]). It's recommended to use
 * [AsyncImage] unless you need a reference to a [Painter].
 *
 * @param model Either an [ImageRequest] or the [ImageRequest.data] value.
 * @param placeholder A [Painter] that is displayed while the image is loading.
 * @param error A [Painter] that is displayed when the image request is unsuccessful.
 * @param fallback A [Painter] that is displayed when the request's [ImageRequest.data] is null.
 * @param onLoading Called when the image request begins loading.
 * @param onSuccess Called when the image request completes successfully.
 * @param onError Called when the image request completes unsuccessfully.
 * @param filterQuality Sampling algorithm applied to a bitmap when it is scaled and drawn
 *  into the destination.
 */
@Composable
fun rememberAsyncImagePainter(
    model: Any?,
    placeholder: Painter? = null,
    error: Painter? = null,
    fallback: Painter? = null,
    onLoading: ((State.Loading) -> Unit)? = null,
    onSuccess: ((State.Success) -> Unit)? = null,
    onError: ((State.Error) -> Unit)? = null,
    filterQuality: FilterQuality = DefaultFilterQuality,
) = rememberAsyncImagePainter(
    model = model,
    imageLoader = LocalImageLoader.current,
    placeholder = placeholder,
    error = error,
    fallback = fallback,
    onLoading = onLoading,
    onSuccess = onSuccess,
    onError = onError,
    filterQuality = filterQuality
)

/**
 * Return an [AsyncImagePainter] that executes an [ImageRequest] asynchronously and renders the result.
 *
 * **This is a lower-level API than [AsyncImage] and may not work as expected in all situations.**
 * **It's highly recommended to use [AsyncImage] unless you need a reference to a [Painter].**
 *
 * Notably, [AsyncImagePainter] will not finish loading if [AsyncImagePainter.onDraw] is not called,
 * which can occur for composables that don't have a fixed size (e.g. [LazyColumn]). Also
 * [AsyncImagePainter.state] will not transition to [State.Success] synchronously during the
 * composition phase.
 *
 * @param model Either an [ImageRequest] or the [ImageRequest.data] value.
 * @param transform A callback to transform a new [State] before it's applied to the
 *  [AsyncImagePainter]. Typically this is used to overwrite the state's [Painter].
 * @param onState Called when the state of this painter changes.
 * @param filterQuality Sampling algorithm applied to a bitmap when it is scaled and drawn
 *  into the destination.
 */
@Composable
fun rememberAsyncImagePainter(
    model: Any?,
    transform: (State) -> State = DefaultTransform,
    onState: ((State) -> Unit)? = null,
    filterQuality: FilterQuality = DefaultFilterQuality,
) = rememberAsyncImagePainter(
    model = model,
    imageLoader = LocalImageLoader.current,
    transform = transform,
    onState = onState,
    filterQuality = filterQuality
)
