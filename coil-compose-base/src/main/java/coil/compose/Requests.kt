package coil.compose

import androidx.compose.ui.graphics.painter.Painter
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

private const val PLACEHOLDER_PAINTER = "coil#placeholder_painter"
private const val ERROR_PAINTER = "coil#error_painter"
private const val FALLBACK_PAINTER = "coil#fallback_painter"
