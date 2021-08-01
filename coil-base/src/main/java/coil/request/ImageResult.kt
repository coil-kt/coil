package coil.request

import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.memory.MemoryCache
import java.io.File

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
     * The cache key for the placeholder in the memory cache.
     * It is 'null' if [ImageRequest.placeholderMemoryCacheKey] is 'null'
     * or if the cache key was not present in the memory cache.
     *
     * @see ImageRequest.Builder.placeholderMemoryCacheKey
     */
    val placeholderMemoryCacheKey: MemoryCache.Key?,

    /**
     * A direct reference to where this image was stored on disk when it was decoded.
     * It is 'null' if the image is not stored on disk.
     *
     * You should treat this file as read-only and not write to it. Also, you should
     * always check [File.exists] before reading the file as it's possible for the
     * file to be moved or deleted at any time.
     */
    val file: File?,

    /**
     * 'true' if the image is sampled (i.e. loaded into memory at less than its original size).
     */
    val isSampled: Boolean,
) : ImageResult() {

    fun copy(
        drawable: Drawable = this.drawable,
        request: ImageRequest = this.request,
        dataSource: DataSource = this.dataSource,
        memoryCacheKey: MemoryCache.Key? = this.memoryCacheKey,
        placeholderMemoryCacheKey: MemoryCache.Key? = this.placeholderMemoryCacheKey,
        file: File? = this.file,
        isSampled: Boolean = this.isSampled,
    ) = SuccessResult(
        drawable = drawable,
        request = request,
        dataSource = dataSource,
        memoryCacheKey = memoryCacheKey,
        placeholderMemoryCacheKey = placeholderMemoryCacheKey,
        file = file,
        isSampled = isSampled,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is SuccessResult &&
            drawable == other.drawable &&
            request == other.request &&
            dataSource == other.dataSource &&
            memoryCacheKey == other.memoryCacheKey &&
            placeholderMemoryCacheKey == other.placeholderMemoryCacheKey &&
            file == other.file &&
            isSampled == other.isSampled
    }

    override fun hashCode(): Int {
        var result = drawable.hashCode()
        result = 31 * result + request.hashCode()
        result = 31 * result + dataSource.hashCode()
        result = 31 * result + (memoryCacheKey?.hashCode() ?: 0)
        result = 31 * result + (placeholderMemoryCacheKey?.hashCode() ?: 0)
        result = 31 * result + (file?.hashCode() ?: 0)
        result = 31 * result + isSampled.hashCode()
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
