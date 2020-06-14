@file:OptIn(ExperimentalCoilApi::class)

package coil.request

import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.memory.MemoryCache

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
        message = "Moved to SuccessResult.Metadata.",
        replaceWith = ReplaceWith("info.source")
    )
    val source: DataSource get() = metadata.source

    /**
     * Supplemental information about the request.
     *
     * @param key The cache key for the image in the memory cache.
     * @param source The data source that the image was loaded from.
     */
    data class Metadata(
        val key: MemoryCache.Key?,
        val source: DataSource
    )
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
