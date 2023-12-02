package coil3.disk

import coil3.ImageLoader
import okio.FileSystem

/**
 * The singleton instance of the disk cache.
 *
 * This instance is used by default by [ImageLoader.Builder] and is necessary to avoid
 * having multiple [DiskCache] instances active in the same directory at the same time.
 */
private val instance by lazy {
    DiskCache.Builder()
        .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "coil3_disk_cache")
        .build()
}

internal actual fun singletonDiskCache(): DiskCache? = instance
