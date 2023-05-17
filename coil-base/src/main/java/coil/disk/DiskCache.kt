package coil.disk

import android.os.StatFs
import androidx.annotation.FloatRange
import coil.annotation.ExperimentalCoilApi
import coil.util.asEditor
import coil.util.asSnapshot
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okio.Closeable
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath

/**
 * An LRU cache of files.
 */
interface DiskCache {

    /** The current size of the cache in bytes. */
    @ExperimentalCoilApi
    val size: Long

    /** The maximum size of the cache in bytes. */
    @ExperimentalCoilApi
    val maxSize: Long

    /** The directory where the cache stores its data. */
    @ExperimentalCoilApi
    val directory: Path

    /** The file system that contains the cache's files. */
    @ExperimentalCoilApi
    val fileSystem: FileSystem

    /**
     * Read the entry associated with [key].
     *
     * IMPORTANT: **You must** call either [Reader.close] or [Reader.closeAndOpenWriter] when
     * finished reading the reader. An open reader prevents writing to the entry or deleting it on
     * disk.
     */
    @ExperimentalCoilApi
    fun openReader(key: String): Reader?

    /**
     * Write to the entry associated with [key].
     *
     * IMPORTANT: **You must** call one of [Writer.commit], [Writer.commitAndOpenReader], or
     * [Writer.abort] to complete the write. An open writer prevents opening new [Reader] or
     * [Writer]s.
     */
    @ExperimentalCoilApi
    fun openWriter(key: String): Writer?

    /**
     * Delete the entry referenced by [key].
     *
     * @return 'true' if [key] was removed successfully. Else, return 'false'.
     */
    @ExperimentalCoilApi
    fun remove(key: String): Boolean

    /** Delete all entries in the disk cache. */
    @ExperimentalCoilApi
    fun clear()

    /**
     * Allows reading the values for an entry.
     *
     * IMPORTANT: You must **only read** [metadata] or [data]. Mutating either file can corrupt the
     * disk cache. To modify the contents of those files, use [openWriter].
     */
    @ExperimentalCoilApi
    interface Reader : Closeable {

        /** Get the metadata file path for this entry. */
        val metadata: Path

        /** Get the data file path for this entry. */
        val data: Path

        /** Close the reader to allow writing. */
        override fun close()

        /** Close the reader and call [openWriter] for this entry atomically. */
        fun closeAndOpenWriter(): Writer?
    }

    /**
     * Allows writing to the values for an entry.
     *
     * Calling [metadata] or [data] marks that file as dirty so it will be persisted to disk
     * if this writer is committed.
     *
     * IMPORTANT: You must **only read or modify the contents** of [metadata] or [data].
     * Renaming, locking, or other mutating file operations can corrupt the disk cache.
     */
    @ExperimentalCoilApi
    interface Writer {

        /** Get the metadata file path for this entry. */
        val metadata: Path

        /** Get the data file path for this entry. */
        val data: Path

        /** Commit the write so the changes are visible to readers. */
        fun commit()

        /** Commit the write and call [openReader] for this entry atomically. */
        fun commitAndOpenReader(): Reader?

        /** Abort the write. Any written data will be discarded. */
        fun abort()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Renamed to 'openReader'.", ReplaceWith("openReader(key)"))
    @ExperimentalCoilApi
    operator fun get(key: String): Snapshot? = openReader(key)?.asSnapshot()

    @Suppress("DEPRECATION")
    @Deprecated("Renamed to 'openWriter'.", ReplaceWith("openWriter(key)"))
    @ExperimentalCoilApi
    fun edit(key: String): Editor? = openWriter(key)?.asEditor()

    @Suppress("DEPRECATION")
    @ExperimentalCoilApi
    @Deprecated("Renamed to 'Reader'.", ReplaceWith("Reader"))
    interface Snapshot : Closeable {
        val metadata: Path
        val data: Path
        override fun close()
        fun closeAndEdit(): Editor?
    }

    @Suppress("DEPRECATION")
    @ExperimentalCoilApi
    @Deprecated("Renamed to 'Writer'.", ReplaceWith("Writer"))
    interface Editor {
        val metadata: Path
        val data: Path
        fun commit()
        fun commitAndGet(): Snapshot?
        fun abort()
    }

    class Builder {

        private var directory: Path? = null
        private var fileSystem = FileSystem.SYSTEM
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
                    val stats = StatFs(directory.toFile().apply { mkdir() }.absolutePath)
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
