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
 * @param isSampled True if [drawable] is sampled (i.e. loaded into memory at less than its original size).
 * @param dataSource The data source that the image was loaded from.
 */
class Metadata(
    val key: MemoryCache.Key?,
    val isSampled: Boolean,
    val dataSource: DataSource
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is Metadata &&
            key == other.key &&
            isSampled == other.isSampled &&
            dataSource == other.dataSource
    }

    override fun hashCode(): Int {
        var result = key?.hashCode() ?: 0
        result = 31 * result + isSampled.hashCode()
        result = 31 * result + dataSource.hashCode()
        return result
    }

    override fun toString(): String {
        return "Metadata(key=$key, isSampled=$isSampled, dataSource=$dataSource)"
    }
}
