package coil3.request

import coil3.Image
import coil3.ImageLoader
import coil3.annotation.Data
import coil3.decode.DataSource
import coil3.memory.MemoryCache

/**
 * Represents the result of an executed [ImageRequest].
 *
 * @see ImageLoader.enqueue
 * @see ImageLoader.execute
 */
sealed interface ImageResult {
    val image: Image?
    val request: ImageRequest
}

/**
 * Indicates that the request completed successfully.
 */
@Data
class SuccessResult(
    /**
     * The success drawable.
     */
    override val image: Image,

    /**
     * The request that was executed to create this result.
     */
    override val request: ImageRequest,

    /**
     * The data source that the image was loaded from.
     */
    val dataSource: DataSource,

    /**
     * The cache key for the image in the memory cache.
     * It is 'null' if the image was not written to the memory cache.
     */
    val memoryCacheKey: MemoryCache.Key? = null,

    /**
     * The cache key for the image in the disk cache.
     * It is 'null' if the image was not written to the disk cache.
     */
    val diskCacheKey: String? = null,

    /**
     * 'true' if the image is sampled (i.e. loaded into memory at less than its original size).
     */
    val isSampled: Boolean = false,

    /**
     * 'true' if [ImageRequest.placeholderMemoryCacheKey] was present in the memory cache.
     */
    val isPlaceholderCached: Boolean = false,
) : ImageResult {

    fun copy(
        image: Image,
        request: ImageRequest,
        dataSource: DataSource,
        memoryCacheKey: MemoryCache.Key? = null,
        diskCacheKey: String? = null,
        isSampled: Boolean = false,
        isPlaceholderCached: Boolean = false,
    ) = SuccessResult(
        image = image,
        request = request,
        dataSource = dataSource,
        memoryCacheKey = memoryCacheKey,
        diskCacheKey = diskCacheKey,
        isSampled = isSampled,
        isPlaceholderCached = isPlaceholderCached,
    )
}

/**
 * Indicates that an error occurred while executing the request.
 */
@Data
class ErrorResult(
    /**
     * The error drawable.
     */
    override val image: Image?,

    /**
     * The request that was executed to create this result.
     */
    override val request: ImageRequest,

    /**
     * The error that failed the request.
     */
    val throwable: Throwable,
) : ImageResult {

    fun copy(
        image: Image? = this.image,
        request: ImageRequest = this.request,
        throwable: Throwable = this.throwable,
    ) = ErrorResult(
        image = image,
        request = request,
        throwable = throwable,
    )
}
