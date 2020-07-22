package coil.request

import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.decode.DataSource

/**
 * Represents the result of an image request.
 *
 * @see ImageLoader.execute
 */
sealed class ImageResult {
    abstract val drawable: Drawable?
    abstract val request: ImageRequest
}

/**
 * Indicates that the request completed successfully.
 *
 * @param drawable The result drawable.
 * @param request The request that was executed to create this result.
 * @param metadata Metadata about the request that created this response.
 */
data class SuccessResult(
    override val drawable: Drawable,
    override val request: ImageRequest,
    val metadata: Metadata
) : ImageResult() {

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
 * @param request The request that was executed to create this result.
 * @param throwable The error that failed the request.
 */
data class ErrorResult(
    override val drawable: Drawable?,
    override val request: ImageRequest,
    val throwable: Throwable
) : ImageResult()
