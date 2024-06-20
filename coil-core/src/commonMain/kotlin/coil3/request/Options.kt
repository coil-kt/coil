package coil3.request

import coil3.Extras
import coil3.PlatformContext
import coil3.annotation.Poko
import coil3.decode.Decoder
import coil3.fetch.Fetcher
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import coil3.util.defaultFileSystem
import okio.FileSystem

/**
 * A set of configuration options for fetching and decoding an image.
 *
 * [Fetcher]s and [Decoder]s should respect these options as best as possible.
 */
@Poko
class Options(
    /**
     * The [PlatformContext] used to execute this request.
     */
    val context: PlatformContext,

    /**
     * The requested output size for the image request.
     */
    val size: Size = Size.ORIGINAL,

    /**
     * The scaling algorithm for how to fit the source image's dimensions into the target's
     * dimensions.
     */
    val scale: Scale = Scale.FIT,

    /**
     * [Precision.EXACT] if the output image needs to fit/fill the target's dimensions exactly.
     *
     * For instance, if [Precision.INEXACT] `BitmapFactoryDecoder` will not decode an image at a
     * larger size than its source dimensions as an optimization.
     */
    val precision: Precision = Precision.EXACT,

    /**
     * The cache key to use when persisting images to the disk cache or 'null' if the component can
     * compute its own.
     */
    val diskCacheKey: String? = null,

    /**
     * The [FileSystem] that will be used to perform any disk read/write operations.
     */
    val fileSystem: FileSystem = defaultFileSystem(),

    /**
     * Determines if this request is allowed to read/write from/to memory.
     */
    val memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,

    /**
     * Determines if this request is allowed to read/write from/to disk.
     */
    val diskCachePolicy: CachePolicy = CachePolicy.ENABLED,

    /**
     * Determines if this request is allowed to read from the network.
     */
    val networkCachePolicy: CachePolicy = CachePolicy.ENABLED,

    /**
     * Extras that are used to configure/extend an image loader's base functionality.
     */
    val extras: Extras = Extras.EMPTY,
) {

    fun copy(
        context: PlatformContext = this.context,
        size: Size = this.size,
        scale: Scale = this.scale,
        precision: Precision = this.precision,
        diskCacheKey: String? = this.diskCacheKey,
        fileSystem: FileSystem = this.fileSystem,
        memoryCachePolicy: CachePolicy = this.memoryCachePolicy,
        diskCachePolicy: CachePolicy = this.diskCachePolicy,
        networkCachePolicy: CachePolicy = this.networkCachePolicy,
        extras: Extras = this.extras,
    ) = Options(
        context = context,
        size = size,
        scale = scale,
        precision = precision,
        diskCacheKey = diskCacheKey,
        fileSystem = fileSystem,
        memoryCachePolicy = memoryCachePolicy,
        diskCachePolicy = diskCachePolicy,
        networkCachePolicy = networkCachePolicy,
        extras = extras,
    )
}
