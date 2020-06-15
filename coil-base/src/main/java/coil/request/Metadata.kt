@file:OptIn(ExperimentalCoilApi::class)

package coil.request

import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.memory.MemoryCache

/**
 * Supplemental information about a successful image request.
 *
 * @param key The cache key for the image in the memory cache.
 *  It is null if the image was not written to the memory cache.
 * @param dataSource The data source that the image was loaded from.
 */
data class Metadata(
    val key: MemoryCache.Key?,
    val dataSource: DataSource
)
