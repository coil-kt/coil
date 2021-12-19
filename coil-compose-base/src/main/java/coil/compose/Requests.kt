@file:Suppress("UNCHECKED_CAST")

package coil.compose

import android.graphics.drawable.Drawable
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
    placeholder(null as Drawable?)
    setParameter(PLACEHOLDER_PAINTER, placeholder, cacheKey = null)
}

/** @see placeholder */
fun Parameters.placeholderPainter(): Painter? = value(PLACEHOLDER_PAINTER)

/**
 * Set a [Painter] to display when the request is unsuccessful.
 *
 * This will only work if this request is executed by [AsyncImage] or [AsyncImagePainter].
 */
fun ImageRequest.Builder.error(error: Painter?) = apply {
    error(null as Drawable?)
    setParameter(ERROR_PAINTER, error, cacheKey = null)
}

/** @see error */
fun Parameters.errorPainter(): Painter? = value(ERROR_PAINTER)

/**
 * Set a [Painter] to display when the request's [ImageRequest.data] is null.
 *
 * This will only work if this request is executed by [AsyncImage] or [AsyncImagePainter].
 */
fun ImageRequest.Builder.fallback(fallback: Painter?) = apply {
    fallback(null as Drawable?)
    setParameter(FALLBACK_PAINTER, fallback, cacheKey = null)
}

/** @see fallback */
fun Parameters.fallbackPainter(): Painter? = value(FALLBACK_PAINTER)

/**
 * Set a callback that is invoked when the image request begins loading.
 *
 * This will only work if this request is executed by [AsyncImage] or [AsyncImagePainter].
 */
fun ImageRequest.Builder.onLoading(callback: (State.Loading) -> Unit) = apply {
    setParameter(LOADING_CALLBACK, callback, cacheKey = null)
}

/** @see onLoading */
fun Parameters.loadingCallback(): ((State.Loading) -> Unit)? = value(LOADING_CALLBACK)

/**
 * Set a callback that is invoked when the image request completes successfully.
 *
 * This will only work if this request is executed by [AsyncImage] or [AsyncImagePainter].
 */
fun ImageRequest.Builder.onSuccess(callback: (State.Success) -> Unit) = apply {
    setParameter(SUCCESS_CALLBACK, callback, cacheKey = null)
}

/** @see onSuccess */
fun Parameters.successCallback(): ((State.Success) -> Unit)? = value(SUCCESS_CALLBACK)

/**
 * Set a callback that is invoked when the image request completes unsuccessfully.
 *
 * This will only work if this request is executed by [AsyncImage] or [AsyncImagePainter].
 */
fun ImageRequest.Builder.onError(callback: (State.Error) -> Unit) = apply {
    setParameter(ERROR_CALLBACK, callback, cacheKey = null)
}

/** @see onError */
fun Parameters.errorCallback(): ((State.Error) -> Unit)? = value(ERROR_CALLBACK)

private const val PLACEHOLDER_PAINTER = "coil#placeholder_painter"
private const val ERROR_PAINTER = "coil#error_painter"
private const val FALLBACK_PAINTER = "coil#fallback_painter"
private const val LOADING_CALLBACK = "coil#loading_callback"
private const val SUCCESS_CALLBACK = "coil#success_callback"
private const val ERROR_CALLBACK = "coil#error_callback"
