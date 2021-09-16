@file:Suppress("unused")

package coil.disk

import android.content.Context
import androidx.annotation.FloatRange
import coil.annotation.ExperimentalCoilApi
import coil.util.Utils
import java.io.File

/**
 * An on-disk cache of previously loaded images.
 */
@ExperimentalCoilApi
interface DiskCache {

    /** The current size of the cache in bytes. */
    val size: Long

    /** The maximum size of the cache in bytes. */
    val maxSize: Long

    /** The keys present in the cache. */
    val keys: Set<String>

    /** The directory where the cache stores its data. */
    val directory: File

    operator fun get(key: String): Snapshot?

    fun edit(key: String): Editor?

    /**
     * Remove the value referenced by [key].
     *
     * @return 'true' if [key] was removed successfully. Else, return 'false'.
     */
    fun remove(key: String): Boolean

    /** Delete all files in the disk cache. */
    fun clear()

    interface Snapshot

    interface Editor

    class Builder(private val context: Context) {

        private var directory = Utils.getDefaultDiskCacheDirectory(context)
        private var maxSizePercent = Utils.DEFAULT_DISK_CACHE_PERCENT
        private var maximumMaxSizeBytes = Utils.DEFAULT_MAX_DISK_CACHE_SIZE_BYTES
        private var minimumMaxSizeBytes = Utils.DEFAULT_MIN_DISK_CACHE_SIZE_BYTES
        private var maxSizeBytes = Long.MIN_VALUE

        fun directory(directory: File) = apply {
            require(directory.isDirectory) { "$directory is not a directory." }
            this.directory = directory
        }

        fun maxSizePercent(@FloatRange(from = 0.0, to = 1.0) percent: Double) = apply {
            require(percent in 0.0..1.0) { "size must be in the range [0.0, 1.0]." }
            this.maxSizeBytes = Long.MIN_VALUE
            this.maxSizePercent = percent
        }

        fun maximumMaxSizeBytes(size: Long) = apply {
            require(size >= 0) { "size must be >= 0." }
            this.maximumMaxSizeBytes = size
        }

        fun minimumMaxSizeBytes(size: Long) = apply {
            require(size >= 0) { "size must be >= 0." }
            this.minimumMaxSizeBytes = size
        }

        fun maxSizeBytes(size: Long) = apply {
            require(size >= 0) { "size must be >= 0." }
            this.maxSizePercent = Double.MIN_VALUE
            this.maxSizeBytes = size
        }

        fun build(): DiskCache = TODO()
    }

    companion object {
        /** Create a new [DiskCache] without configuration. */
        @JvmStatic
        @JvmName("create")
        operator fun invoke(context: Context) = Builder(context).build()
    }
}
