@file:Suppress("unused")

package coil.disk

import android.os.StatFs
import androidx.annotation.FloatRange
import coil.annotation.ExperimentalCoilApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okio.Closeable
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File

/**
 * An LRU cache of files.
 */
@ExperimentalCoilApi
interface DiskCache {

    /** The current size of the cache in bytes. */
    val size: Long

    /** The maximum size of the cache in bytes. */
    val maxSize: Long

    /** The directory where the cache stores its data. */
    val directory: Path

    /** The file system that contains the cache's files. */
    val fileSystem: FileSystem

    /**
     * Get the entry associated with [key].
     *
     * IMPORTANT: **You must** call either [Snapshot.close] or [Snapshot.closeAndEdit] when finished
     * reading the snapshot. An open snapshot prevents editing the entry or deleting it on disk.
     */
    operator fun get(key: String): Snapshot?

    /**
     * Edit the entry associated with [key].
     *
     * IMPORTANT: **You must** call one of [Editor.commit], [Editor.commitAndGet], or [Editor.abort]
     * to complete the edit. An open editor prevents opening new [Snapshot]s or opening a new
     * [Editor].
     */
    fun edit(key: String): Editor?

    /**
     * Delete the entry referenced by [key].
     *
     * @return 'true' if [key] was removed successfully. Else, return 'false'.
     */
    fun remove(key: String): Boolean

    /** Delete all entries in the disk cache. */
    fun clear()

    /**
     * A snapshot of the values for an entry.
     *
     * IMPORTANT: You must **only read** [metadata] or [data]. Mutating either file can corrupt the
     * disk cache. To modify the contents of those files, use [edit].
     */
    interface Snapshot : Closeable {

        /** Get the metadata for this entry. */
        val metadata: Path

        /** Get the data for this entry. */
        val data: Path

        /** Close the snapshot to allow editing. */
        override fun close()

        /** Close the snapshot and call [edit] for this entry atomically. */
        fun closeAndEdit(): Editor?
    }

    /**
     * Edits the values for an entry.
     *
     * Calling [metadata] or [data] marks that file as dirty so it will be persisted to disk
     * if this editor is committed.
     *
     * IMPORTANT: You must **only read or modify the contents** of [metadata] or [data].
     * Renaming, locking, or other mutating file operations can corrupt the disk cache.
     */
    interface Editor {

        /** Get the metadata for this entry. */
        val metadata: Path

        /** Get the data for this entry. */
        val data: Path

        /** Commit the edit so the changes are visible to readers. */
        fun commit()

        /** Commit the edit and open a new [Snapshot] atomically. */
        fun commitAndGet(): Snapshot?

        /** Abort the edit. Any written data will be discarded. */
        fun abort()
    }

    class Builder {

        private var directory: Path? = null
        private var fileSystem: FileSystem = FileSystem.SYSTEM
        private var maxSizePercent = 0.02 // 2%
        private var minimumMaxSizeBytes = 10L * 1024 * 1024 // 10MB
        private var maximumMaxSizeBytes = 250L * 1024 * 1024 // 250MB
        private var maxSizeBytes = 0L
        private var cleanupDispatcher = Dispatchers.IO

        /**
         * Set the [directory] where the cache stores its data.
         *
         * IMPORTANT: It is an error to have two [DiskCache] instances active in the same
         * directory at the same time as this can corrupt the disk cache.
         */
        fun directory(directory: File) = directory(directory.toOkioPath())

        /**
         * Set the [directory] where the cache stores its data.
         *
         * IMPORTANT: It is an error to have two [DiskCache] instances active in the same
         * directory at the same time as this can corrupt the disk cache.
         */
        fun directory(directory: Path) = apply {
            this.directory = directory
        }

        /**
         * Set the [fileSystem] where the cache stores its data, usually [FileSystem.SYSTEM].
         */
        fun fileSystem(fileSystem: FileSystem) = apply {
            this.fileSystem = fileSystem
        }

        /**
         * Set the maximum size of the disk cache as a percentage of the device's free disk space.
         */
        fun maxSizePercent(@FloatRange(from = 0.0, to = 1.0) percent: Double) = apply {
            require(percent in 0.0..1.0) { "size must be in the range [0.0, 1.0]." }
            this.maxSizeBytes = 0
            this.maxSizePercent = percent
        }

        /**
         * Set the minimum size of the disk cache in bytes.
         * This is ignored if [maxSizeBytes] is set.
         */
        fun minimumMaxSizeBytes(size: Long) = apply {
            require(size > 0) { "size must be > 0." }
            this.minimumMaxSizeBytes = size
        }

        /**
         * Set the maximum size of the disk cache in bytes.
         * This is ignored if [maxSizeBytes] is set.
         */
        fun maximumMaxSizeBytes(size: Long) = apply {
            require(size > 0) { "size must be > 0." }
            this.maximumMaxSizeBytes = size
        }

        /**
         * Set the maximum size of the disk cache in bytes.
         */
        fun maxSizeBytes(size: Long) = apply {
            require(size > 0) { "size must be > 0." }
            this.maxSizePercent = 0.0
            this.maxSizeBytes = size
        }

        /**
         * Set the [CoroutineDispatcher] that cache size trim operations will be executed on.
         */
        fun cleanupDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.cleanupDispatcher = dispatcher
        }

        /**
         * Create a new [DiskCache] instance.
         */
        fun build(): DiskCache {
            val directory = checkNotNull(directory) { "directory == null" }
            val maxSize = if (maxSizePercent > 0) {
                try {
                    val stats = StatFs(directory.toFile().absolutePath)
                    val size = maxSizePercent * stats.blockCountLong * stats.blockSizeLong
                    size.toLong().coerceIn(minimumMaxSizeBytes, maximumMaxSizeBytes)
                } catch (_: Exception) {
                    minimumMaxSizeBytes
                }
            } else {
                maxSizeBytes
            }
            return RealDiskCache(
                maxSize = maxSize,
                directory = directory,
                fileSystem = fileSystem,
                cleanupDispatcher = cleanupDispatcher
            )
        }
    }
}
