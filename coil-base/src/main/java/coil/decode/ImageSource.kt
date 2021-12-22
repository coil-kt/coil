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
import okio.buffer
import okio.sink
import okio.source
import java.io.Closeable
import java.io.File

/**
 * Create a new [ImageSource] backed by a [File].
 *
 * @param file The file to read from.
 * @param diskCacheKey An optional cache key for the [file] in the disk cache.
 * @param closeable An optional closeable reference that will
 *  be closed when the image source is closed.
 */
@JvmName("create")
fun ImageSource(
    file: File,
    diskCacheKey: String? = null,
    closeable: Closeable? = null,
): ImageSource = FileImageSource(file, diskCacheKey, closeable, null)

/**
 * Create a new [ImageSource] backed by a [File].
 *
 * @param file The file to read from.
 * @param diskCacheKey An optional cache key for the [file] in the disk cache.
 * @param closeable An optional closeable reference that will
 *  be closed when the image source is closed.
 * @param metadata Metadata about this image source.
 */
@ExperimentalCoilApi
@JvmName("create")
fun ImageSource(
    file: File,
    diskCacheKey: String? = null,
    closeable: Closeable? = null,
    metadata: ImageSource.Metadata? = null,
): ImageSource = FileImageSource(file, diskCacheKey, closeable, metadata)

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
 * @param metadata Metadata about this image source.
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
 * @param cacheDirectory The directory to create temporary files in
 *  if [ImageSource.file] is called.
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
 * @param cacheDirectory The directory to create temporary files in
 *  if [ImageSource.file] is called.
 * @param metadata Metadata about this image source.
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
     * Return a [File] containing this [ImageSource]'s data.
     * If this image source is backed by a [BufferedSource], a temporary file containing this
     * [ImageSource]'s data will be created.
     */
    abstract fun file(): File

    /**
     * Return a [File] containing this [ImageSource]'s data if one has already been created.
     * Else, return 'null'.
     */
    abstract fun fileOrNull(): File?

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
 * Metadata containing the [packageName] and [resId] of an Android resource.
 */
@ExperimentalCoilApi
class ResourceMetadata(
    val packageName: String,
    @DrawableRes val resId: Int
) : ImageSource.Metadata()

internal class FileImageSource(
    internal val file: File,
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
        return file.source().buffer().also { source = it }
    }

    @Synchronized
    override fun sourceOrNull(): BufferedSource? {
        assertNotClosed()
        return source
    }

    @Synchronized
    override fun file(): File {
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
    private var file: File? = null

    init {
        require(cacheDirectory.isDirectory) { "cacheDirectory must be a directory." }
    }

    @Synchronized
    override fun source(): BufferedSource {
        assertNotClosed()
        source?.let { return it }
        return file!!.source().buffer().also { source = it }
    }

    override fun sourceOrNull() = source()

    @Synchronized
    override fun file(): File {
        assertNotClosed()
        file?.let { return it }

        // Copy the source to a temp file.
        val tempFile = File.createTempFile("tmp", null, cacheDirectory)
        source!!.use { tempFile.sink().use(it::readAll) }
        source = null
        return tempFile.also { file = it }
    }

    @Synchronized
    override fun fileOrNull(): File? {
        assertNotClosed()
        return file
    }

    @Synchronized
    override fun close() {
        isClosed = true
        source?.closeQuietly()
        file?.delete()
    }

    private fun assertNotClosed() {
        check(!isClosed) { "closed" }
    }
}
