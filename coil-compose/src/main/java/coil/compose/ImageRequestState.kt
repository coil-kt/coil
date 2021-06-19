package coil.compose

import androidx.compose.ui.graphics.painter.Painter
import coil.decode.DataSource

/**
 * Represents the state of a [LoadPainter].
 */
sealed class ImageLoadState {

    /**
     * Indicates that a request is not in progress.
     */
    object Empty : ImageLoadState()

    /**
     * Indicates that the request is currently in progress.
     */
    data class Loading(
        val placeholder: Painter?,
        val request: Any,
    ) : ImageLoadState()

    /**
     * Indicates that the request completed successfully.
     *
     * @param result The result [Painter].
     * @param source The data source that the image was loaded from.
     * @param request The original request for this result.
     */
    data class Success(
        val result: Painter,
        val source: DataSource,
        val request: Any,
    ) : ImageLoadState()

    /**
     * Indicates that an error occurred while executing the request.
     *
     * @param result The error image.
     * @param throwable The optional throwable that caused the request failure.
     * @param request The original request for this result.
     */
    data class Error(
        val request: Any,
        val result: Painter? = null,
        val throwable: Throwable? = null
    ) : ImageLoadState()
}

/**
 * Returns true if this state represents the final state for the current request.
 */
fun ImageLoadState.isFinalState(): Boolean {
    return this is ImageLoadState.Success || this is ImageLoadState.Error
}

internal inline val ImageLoadState.painter: Painter?
    get() = when (this) {
        is ImageLoadState.Success -> result
        is ImageLoadState.Error -> result
        is ImageLoadState.Loading -> placeholder
        else -> null
    }

internal inline val ImageLoadState.request: Any?
    get() = when (this) {
        is ImageLoadState.Success -> request
        is ImageLoadState.Error -> request
        else -> null
    }
