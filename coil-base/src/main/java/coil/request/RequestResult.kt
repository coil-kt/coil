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
 * Indicates that the request completed successfully.
 *
 * @param drawable The result drawable.
 * @param metadata Metadata about the request that created this response.
 */
data class SuccessResult(
    override val drawable: Drawable,
    val metadata: Metadata
) : RequestResult() {

    @Deprecated(
        message = "Moved to Metadata.",
        replaceWith = ReplaceWith("metadata.dataSource")
    )
    val source: DataSource get() = metadata.dataSource
}

/**
 * Indicates that an error occurred while executing the request.
 *
 * @param drawable The error drawable.
 * @param throwable The error that failed the request.
 */
data class ErrorResult(
    override val drawable: Drawable?,
    val throwable: Throwable
) : RequestResult()
