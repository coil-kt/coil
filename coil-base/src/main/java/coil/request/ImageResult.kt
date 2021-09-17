package coil.request

import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.memory.MemoryCache

/**
 * Represents the result of an executed [ImageRequest].
 *
 * @see ImageLoader.enqueue
 * @see ImageLoader.execute
 */
sealed class ImageResult {
    abstract val drawable: Drawable?
    abstract val request: ImageRequest
}

/**
 * Indicates that the request completed successfully.
 */
class SuccessResult(
    /**
     * The success drawable.
     */
    override val drawable: Drawable,

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
    val memoryCacheKey: MemoryCache.Key?,

    /**
     * The cache key for the image in the disk cache.
     * It is 'null' if the image was not written to the disk cache.
     */
    val diskCacheKey: String?,

    /**
     * 'true' if the image is sampled (i.e. loaded into memory at less than its original size).
     */
    val isSampled: Boolean,

    /**
     * 'true' if [ImageRequest.placeholderMemoryCacheKey] was present in the memory cache.
     */
    val isPlaceholderCached: Boolean
) : ImageResult() {

    fun copy(
        drawable: Drawable = this.drawable,
        request: ImageRequest = this.request,
        dataSource: DataSource = this.dataSource,
        memoryCacheKey: MemoryCache.Key? = this.memoryCacheKey,
        diskCacheKey: String? = this.diskCacheKey,
        isSampled: Boolean = this.isSampled,
        isPlaceholderCached: Boolean = this.isPlaceholderCached,
    ) = SuccessResult(
        drawable = drawable,
        request = request,
        dataSource = dataSource,
        memoryCacheKey = memoryCacheKey,
        diskCacheKey = diskCacheKey,
        isSampled = isSampled,
        isPlaceholderCached = isPlaceholderCached,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is SuccessResult &&
            drawable == other.drawable &&
            request == other.request &&
            dataSource == other.dataSource &&
            memoryCacheKey == other.memoryCacheKey &&
            diskCacheKey == other.diskCacheKey &&
            isSampled == other.isSampled &&
            isPlaceholderCached == other.isPlaceholderCached
    }

    override fun hashCode(): Int {
        var result = drawable.hashCode()
        result = 31 * result + request.hashCode()
        result = 31 * result + dataSource.hashCode()
        result = 31 * result + (memoryCacheKey?.hashCode() ?: 0)
        result = 31 * result + (diskCacheKey?.hashCode() ?: 0)
        result = 31 * result + isSampled.hashCode()
        result = 31 * result + isPlaceholderCached.hashCode()
        return result
    }
}

/**
 * Indicates that an error occurred while executing the request.
 */
class ErrorResult(
    /**
     * The error drawable.
     */
    override val drawable: Drawable?,

    /**
     * The request that was executed to create this result.
     */
    override val request: ImageRequest,

    /**
     * The error that failed the request.
     */
    val throwable: Throwable,
) : ImageResult() {

    fun copy(
        drawable: Drawable? = this.drawable,
        request: ImageRequest = this.request,
        throwable: Throwable = this.throwable,
    ) = ErrorResult(
        drawable = drawable,
        request = request,
        throwable = throwable,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is ErrorResult &&
            drawable == other.drawable &&
            request == other.request &&
            throwable == other.throwable
    }

    override fun hashCode(): Int {
        var result = drawable?.hashCode() ?: 0
        result = 31 * result + request.hashCode()
        result = 31 * result + throwable.hashCode()
        return result
    }
}
