package coil.request

import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.decode.DataSource

/**
 * Represents the result of an image request.
 *
 * @see ImageLoader.execute
 */
sealed class RequestResult {
    abstract val drawable: Drawable?
}

/**
 * Denotes that the request completed successfully.
 *
 * @param drawable The result drawable.
 * @param source The data source that the image was loaded from.
 */
data class SuccessResult(
    override val drawable: Drawable,
    val source: DataSource
) : RequestResult()

/**
 * Denotes that an error occurred while executing the request.
 *
 * @param drawable The error drawable.
 * @param throwable The error that failed the request.
 */
data class ErrorResult(
    override val drawable: Drawable?,
    val throwable: Throwable
) : RequestResult()
