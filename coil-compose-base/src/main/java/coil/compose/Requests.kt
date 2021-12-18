@file:Suppress("UNCHECKED_CAST")

package coil.compose

import androidx.compose.ui.graphics.painter.Painter
import coil.compose.AsyncImagePainter.State
import coil.request.ImageRequest
import coil.request.Parameters

/**
 * Set a [Painter] to display while the request is loading.
 *
 * This will only work if this request is executed by [AsyncImage] or [AsyncImagePainter].
 */
fun ImageRequest.Builder.placeholder(placeholder: Painter?) = apply {
    setParameter(PLACEHOLDER_PAINTER, placeholder, cacheKey = null)
}

/**
 * Set a [Painter] to display when the request is unsuccessful.
 *
 * This will only work if this request is executed by [AsyncImage] or [AsyncImagePainter].
 */
fun ImageRequest.Builder.error(error: Painter?) = apply {
    setParameter(ERROR_PAINTER, error, cacheKey = null)
}

/**
 * Set a [Painter] to display when the request's [ImageRequest.data] is null.
 *
 * This will only work if this request is executed by [AsyncImage] or [AsyncImagePainter].
 */
fun ImageRequest.Builder.fallback(fallback: Painter?) = apply {
    setParameter(FALLBACK_PAINTER, fallback, cacheKey = null)
}

fun ImageRequest.Builder.onLoading(callback: (ImageRequest, State.Loading) -> Unit) = apply {
    setParameter(LOADING_CALLBACK, callback, cacheKey = null)
}

fun ImageRequest.Builder.onSuccess(callback: (ImageRequest, State.Success) -> Unit) = apply {
    setParameter(SUCCESS_CALLBACK, callback, cacheKey = null)
}

fun ImageRequest.Builder.onError(callback: (ImageRequest, State.Error) -> Unit) = apply {
    setParameter(ERROR_CALLBACK, callback, cacheKey = null)
}

/**
 * Get the placeholder painter.
 */
fun Parameters.placeholder(): Painter? = value(PLACEHOLDER_PAINTER) as Painter?

/**
 * Get the error painter.
 */
fun Parameters.error(): Painter? = value(ERROR_PAINTER) as Painter?

/**
 * Get the fallback painter.
 */
fun Parameters.fallback(): Painter? = value(FALLBACK_PAINTER) as Painter?

fun Parameters.loadingCallback(): ((ImageRequest, State.Loading) -> Unit)? =
    value(LOADING_CALLBACK) as ((ImageRequest, State.Loading) -> Unit)?

fun Parameters.successCallback(): ((ImageRequest, State.Success) -> Unit)? =
    value(LOADING_CALLBACK) as ((ImageRequest, State.Success) -> Unit)?

fun Parameters.errorCallback(): ((ImageRequest, State.Error) -> Unit)? =
    value(LOADING_CALLBACK) as ((ImageRequest, State.Error) -> Unit)?

private const val PLACEHOLDER_PAINTER = "coil#placeholder_painter"
private const val ERROR_PAINTER = "coil#error_painter"
private const val FALLBACK_PAINTER = "coil#fallback_painter"
private const val LOADING_CALLBACK = "coil#loading_callback"
private const val SUCCESS_CALLBACK = "coil#success_callback"
private const val ERROR_CALLBACK = "coil#error_callback"
