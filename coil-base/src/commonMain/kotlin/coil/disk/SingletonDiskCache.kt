package coil.disk

import coil.ImageLoader
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Holds the singleton instance of the disk cache. We need to have a singleton disk cache
 * instance to support creating multiple [ImageLoader]s that share the same disk cache directory.
 *
 * @see DiskCache.Builder.directory
 */
internal object SingletonDiskCache : SynchronizedObject() {
    private var instance: DiskCache? = null

    fun get(context: Context): DiskCache = synchronized(this) {
        return instance ?: run {
            // Create the singleton disk cache instance.
            DiskCache.Builder()
                .directory(context.safeCacheDir.resolve("image_cache"))
                .build()
                .also { instance = it }
        }
    }
}
