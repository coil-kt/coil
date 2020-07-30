package coil.request

import coil.decode.DataSource
import coil.memory.MemoryCache

/**
 * Supplemental information about a successful image request.
 *
 * @param memoryCacheKey The cache key for the image in the memory cache.
 *  It is null if the image was not written to the memory cache.
 * @param isSampled True if [drawable] is sampled (i.e. loaded into memory at less than its original size).
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
