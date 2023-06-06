package coil.decode

import coil.ImageLoader
import coil.fetch.DrawableResult
import coil.fetch.SourceResult

/**
 * Represents the source that an image was loaded from.
 *
 * @see SourceResult.dataSource
 * @see DrawableResult.dataSource
 */
enum class DataSource {

    /**
     * Represents an [ImageLoader]'s memory cache.
     *
     * This is a special data source as it means the request was
     * short circuited and skipped the full image pipeline.
     */
    MEMORY_CACHE,

    /**
     * Represents an in-memory data source (e.g. `Bitmap`, `ByteBuffer`).
     */
    MEMORY,

    /**
     * Represents a disk-based data source (e.g. `DrawableRes`, `File`).
     */
    DISK,

    /**
     * Represents a network-based data source (e.g. `HttpUrl`).
     */
    NETWORK
}
