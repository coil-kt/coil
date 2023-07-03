package coil.disk

import coil.ImageLoader
import okio.FileSystem

/**
 * The singleton instance of the disk cache.
 *
 * This instance is used by default by [ImageLoader.Builder] and is necessary to avoid
 * having multiple [DiskCache] instances active in the same directory at the same time.
 */
private val INSTANCE by lazy {
    DiskCache.Builder()
        .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "coil_v3_image_cache")
        .build()
}

internal actual fun singletonDiskCache(): DiskCache? = INSTANCE
