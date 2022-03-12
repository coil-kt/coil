@file:JvmName("ImageSources")

package coil.decode

import android.content.Context
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.DrawableRes
import coil.annotation.ExperimentalCoilApi
import coil.fetch.Fetcher
import coil.util.closeQuietly
import coil.util.safeCacheDir
import okio.BufferedSource
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.source
import java.io.Closeable
import java.io.File

/**
 * Create a new [ImageSource] backed by a [File].
 *
 * @param file The file to read from.
 * @param fileSystem The file system which contains [file].
 * @param diskCacheKey An optional cache key for the [file] in the disk cache.
 * @param closeable An optional closeable reference that will be closed when the image source is closed.
 */
@JvmName("create")
fun ImageSource(
    file: Path,
    fileSystem: FileSystem = FileSystem.SYSTEM,
    diskCacheKey: String? = null,
    closeable: Closeable? = null,
): ImageSource = FileImageSource(file, fileSystem, diskCacheKey, closeable, null)

/**
 * Create a new [ImageSource] backed by a [File].
 *
 * @param file The file to read from.
 * @param fileSystem The file system which contains [file].
 * @param diskCacheKey An optional cache key for the [file] in the disk cache.
 * @param closeable An optional closeable reference that will be closed when the image source is closed.
 * @param metadata Metadata for this image source.
 */
@ExperimentalCoilApi
@JvmName("create")
fun ImageSource(
    file: Path,
    fileSystem: FileSystem = FileSystem.SYSTEM,
    diskCacheKey: String? = null,
    closeable: Closeable? = null,
    metadata: ImageSource.Metadata? = null,
): ImageSource = FileImageSource(file, fileSystem, diskCacheKey, closeable, metadata)

/**
 * Create a new [ImageSource] backed by a [BufferedSource].
 *
 * @param source The buffered source to read from.
 * @param context A context used to resolve a safe cache directory.
 */
@JvmName("create")
fun ImageSource(
    source: BufferedSource,
    context: Context,
): ImageSource = SourceImageSource(source, context.safeCacheDir, null)

/**
 * Create a new [ImageSource] backed by a [BufferedSource].
 *
 * @param source The buffered source to read from.
 * @param context A context used to resolve a safe cache directory.
 * @param metadata Metadata for this image source.
 */
@ExperimentalCoilApi
@JvmName("create")
fun ImageSource(
    source: BufferedSource,
    context: Context,
    metadata: ImageSource.Metadata? = null,
): ImageSource = SourceImageSource(source, context.safeCacheDir, metadata)

/**
 * Create a new [ImageSource] backed by a [BufferedSource].
 *
 * @param source The buffered source to read from.
 * @param cacheDirectory The directory to create temporary files in if [ImageSource.file] is called.
 */
@JvmName("create")
fun ImageSource(
    source: BufferedSource,
    cacheDirectory: File,
): ImageSource = SourceImageSource(source, cacheDirectory, null)

/**
 * Create a new [ImageSource] backed by a [BufferedSource].
 *
 * @param source The buffered source to read from.
 * @param cacheDirectory The directory to create temporary files in if [ImageSource.file] is called.
 * @param metadata Metadata for this image source.
 */
@ExperimentalCoilApi
@JvmName("create")
fun ImageSource(
    source: BufferedSource,
    cacheDirectory: File,
    metadata: ImageSource.Metadata? = null,
): ImageSource = SourceImageSource(source, cacheDirectory, metadata)

/**
 * Provides access to the image data to be decoded.
 */
sealed class ImageSource : Closeable {

    /**
     * The [FileSystem] which contains the [file].
     */
    abstract val fileSystem: FileSystem

    /**
     * Return the [Metadata] for this [ImageSource].
     */
    @ExperimentalCoilApi
    abstract val metadata: Metadata?

    /**
     * Return a [BufferedSource] to read this [ImageSource].
     */
    abstract fun source(): BufferedSource

    /**
     * Return the [BufferedSource] to read this [ImageSource] if one has already been created.
     * Else, return 'null'.
     */
    abstract fun sourceOrNull(): BufferedSource?

    /**
     * Return a [Path] that resolves to a file containing this [ImageSource]'s data.
     *
     * If this image source is backed by a [BufferedSource], a temporary file containing this
     * [ImageSource]'s data will be created.
     */
    abstract fun file(): Path

    /**
     * Return a [Path] that resolves to a file containing this [ImageSource]'s data if one has
     * already been created. Else, return 'null'.
     */
    abstract fun fileOrNull(): Path?

    /**
     * A marker class for metadata for an [ImageSource].
     *
     * **Heavily prefer** using [source] or [file] to decode the image's data instead of relying
     * on information provided in the metadata. It's the responsibility of a [Fetcher] to create a
     * [BufferedSource] or [File] that can be easily read irrespective of where the image data is
     * located. A [Decoder] should be as decoupled as possible from where the image is being fetched
     * from.
     *
     * This method is provided as a way to pass information to decoders that don't support decoding
     * a [BufferedSource] and want to avoid creating a temporary file (e.g. [ImageDecoder],
     * [MediaMetadataRetriever], etc.).
     *
     * @see AssetMetadata
     * @see ContentMetadata
     * @see ResourceMetadata
     */
    @ExperimentalCoilApi
    abstract class Metadata
}

/**
 * Metadata containing the [fileName] of an Android asset.
 */
@ExperimentalCoilApi
class AssetMetadata(val fileName: String) : ImageSource.Metadata()

/**
 * Metadata containing the [uri] of `content` URI.
 */
@ExperimentalCoilApi
class ContentMetadata(val uri: Uri) : ImageSource.Metadata()

/**
 * Metadata containing the [packageName], [resId], and [density] of an Android resource.
 */
@ExperimentalCoilApi
class ResourceMetadata(
    val packageName: String,
    @DrawableRes val resId: Int,
    val density: Int
) : ImageSource.Metadata()

internal class FileImageSource(
    internal val file: Path,
    override val fileSystem: FileSystem,
    internal val diskCacheKey: String?,
    private val closeable: Closeable?,
    override val metadata: Metadata?
) : ImageSource() {

    private var isClosed = false
    private var source: BufferedSource? = null

    @Synchronized
    override fun source(): BufferedSource {
        assertNotClosed()
        source?.let { return it }
        return fileSystem.source(file).buffer().also { source = it }
    }

    @Synchronized
    override fun sourceOrNull(): BufferedSource? {
        assertNotClosed()
        return source
    }

    @Synchronized
    override fun file(): Path {
        assertNotClosed()
        return file
    }

    override fun fileOrNull() = file()

    @Synchronized
    override fun close() {
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
    private val cacheDirectory: File,
    override val metadata: Metadata?
) : ImageSource() {

    private var isClosed = false
    private var source: BufferedSource? = source
    private var file: Path? = null

    init {
        require(cacheDirectory.isDirectory) { "cacheDirectory must be a directory." }
    }

    override val fileSystem get() = FileSystem.SYSTEM

    @Synchronized
    override fun source(): BufferedSource {
        assertNotClosed()
        source?.let { return it }
        return fileSystem.source(file!!).buffer().also { source = it }
    }

    override fun sourceOrNull() = source()

    @Synchronized
    override fun file(): Path {
        assertNotClosed()
        file?.let { return it }

        // Copy the source to a temp file.
        // Replace JVM call with https://github.com/square/okio/issues/1090 once it's available.
        val tempFile = File.createTempFile("tmp", null, cacheDirectory).toOkioPath()
        fileSystem.write(tempFile) {
            writeAll(source!!)
        }
        source = null
        return tempFile.also { file = it }
    }

    @Synchronized
    override fun fileOrNull(): Path? {
        assertNotClosed()
        return file
    }

    @Synchronized
    override fun close() {
        isClosed = true
        source?.closeQuietly()
        file?.let(fileSystem::delete)
    }

    private fun assertNotClosed() {
        check(!isClosed) { "closed" }
    }
}
