package coil.request

import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.memory.MemoryCache

/**
 * Represents the result of an image request.
 *
 * @see ImageLoader.execute
 */
sealed class ImageResult {

    abstract val drawable: Drawable?
    abstract val request: ImageRequest

    /**
     * Supplemental information about a successful image request.
     *
     * @param memoryCacheKey The cache key for the image in the memory cache.
     *  It is null if the image was not written to the memory cache.
     * @param isSampled True if the image is sampled (i.e. loaded into memory at less than its original size).
     * @param dataSource The data source that the image was loaded from.
     * @param isPlaceholderMemoryCacheKeyPresent True if the request's [ImageRequest.placeholderMemoryCacheKey] was
     *  present in the memory cache and was set as the placeholder.
     */
    data class Metadata(
        val memoryCacheKey: MemoryCache.Key?,
        val isSampled: Boolean,
        val dataSource: DataSource,
        val isPlaceholderMemoryCacheKeyPresent: Boolean
    )
}

/**
 * Indicates that the request completed successfully.
 *
 * @param drawable The success drawable.
 * @param request The request that was executed to create this result.
 * @param metadata Metadata about the request that created this response.
 */
data class SuccessResult(
    override val drawable: Drawable,
    override val request: ImageRequest,
    val metadata: Metadata
) : ImageResult() {

    @Deprecated("Moved to Metadata.", ReplaceWith("metadata.dataSource"))
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
