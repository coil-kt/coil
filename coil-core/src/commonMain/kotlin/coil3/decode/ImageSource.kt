package coil3.decode

import coil3.decode.ImageSource.Metadata
import coil3.fetch.Fetcher
import coil3.util.closeQuietly
import coil3.util.createTempFile
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import okio.BufferedSource
import okio.Closeable
import okio.FileSystem
import okio.Path
import okio.buffer

/**
 * Create a new [ImageSource] backed by a [Path].
 *
 * @param file The file to read from.
 * @param fileSystem The file system which contains [file].
 * @param diskCacheKey An optional cache key for the [file] in the disk cache.
 * @param closeable An optional closeable reference that will be closed when the image source is closed.
 * @param metadata Metadata for this image source.
 */
fun ImageSource(
    file: Path,
    fileSystem: FileSystem,
    diskCacheKey: String? = null,
    closeable: Closeable? = null,
    metadata: Metadata? = null,
): ImageSource = FileImageSource(file, fileSystem, diskCacheKey, closeable, metadata)

/**
 * Create a new [ImageSource] backed by a [BufferedSource].
 *
 * @param source The buffered source to read from.
 * @param fileSystem The file system which will be used to create a temporary file if necessary.
 * @param metadata Metadata for this image source.
 */
fun ImageSource(
    source: BufferedSource,
    fileSystem: FileSystem,
    metadata: Metadata? = null,
): ImageSource = SourceImageSource(source, fileSystem, metadata)

/**
 * Provides access to the image data to be decoded.
 */
sealed interface ImageSource : Closeable {

    /**
     * The [FileSystem] which contains the [file].
     */
    val fileSystem: FileSystem

    /**
     * Return the [Metadata] for this [ImageSource].
     */
    val metadata: Metadata?

    /**
     * Return a [BufferedSource] to read this [ImageSource].
     */
    fun source(): BufferedSource

    /**
     * Return the [BufferedSource] to read this [ImageSource] if one has already been created.
     * Else, return 'null'.
     */
    fun sourceOrNull(): BufferedSource?

    /**
     * Return a [Path] that resolves to a file containing this [ImageSource]'s data.
     *
     * If this image source is backed by a [BufferedSource], a temporary file containing this
     * [ImageSource]'s data will be created.
     */
    fun file(): Path

    /**
     * Return a [Path] that resolves to a file containing this [ImageSource]'s data if one has
     * already been created. Else, return 'null'.
     */
    fun fileOrNull(): Path?

    /**
     * A marker class for metadata for an [ImageSource].
     *
     * **Heavily prefer** using [source] or [file] to decode the image's data instead of relying
     * on information provided in the metadata. It's the responsibility of a [Fetcher] to create a
     * [BufferedSource] or [Path] that can be easily read irrespective of where the image data is
     * located. A [Decoder] should be as decoupled as possible from where the image is being fetched
     * from.
     *
     * This method is provided as a way to pass information to decoders that don't support decoding
     * a [BufferedSource] and want to avoid creating a temporary file (e.g. `ImageDecoder`,
     * `MediaMetadataRetriever`).
     */
    abstract class Metadata
}

internal class FileImageSource(
    internal val file: Path,
    override val fileSystem: FileSystem,
    internal val diskCacheKey: String?,
    private val closeable: Closeable?,
    override val metadata: Metadata?,
) : ImageSource {

    private val lock = SynchronizedObject()
    private var isClosed = false
    private var source: BufferedSource? = null

    override fun source(): BufferedSource = synchronized(lock) {
        assertNotClosed()
        source?.let { return it }
        return fileSystem.source(file).buffer().also { source = it }
    }

    override fun sourceOrNull(): BufferedSource? = synchronized(lock) {
        assertNotClosed()
        return source
    }

    override fun file(): Path = synchronized(lock) {
        assertNotClosed()
        return file
    }

    override fun fileOrNull() = file()

    override fun close(): Unit = synchronized(lock) {
        isClosed = true
        source?.closeQuietly()
        closeable?.closeQuietly()
    }

    private fun assertNotClosed() {
        check(!isClosed) { "closed" }
    }
}

internal class SourceImageSource(
    source: BufferedSource,
    override val fileSystem: FileSystem,
    override val metadata: Metadata?,
) : ImageSource {

    private val lock = SynchronizedObject()
    private var isClosed = false
    private var source: BufferedSource? = source
    private var file: Path? = null

    override fun source(): BufferedSource = synchronized(lock) {
        assertNotClosed()
        source?.let { return it }
        return fileSystem.source(file!!).buffer().also { source = it }
    }

    override fun sourceOrNull() = source()

    override fun file(): Path = synchronized(lock) {
        assertNotClosed()
        file?.let { return it }

        // Copy the source to a temp file.
        val tempFile = fileSystem.createTempFile()
        fileSystem.write(tempFile) {
            writeAll(source!!)
        }
        source = null
        return tempFile.also { file = it }
    }

    override fun fileOrNull(): Path? = synchronized(lock) {
        assertNotClosed()
        return file
    }

    override fun close(): Unit = synchronized(lock) {
        isClosed = true
        source?.closeQuietly()
        file?.let(fileSystem::delete)
    }

    private fun assertNotClosed() {
        check(!isClosed) { "closed" }
    }
}
