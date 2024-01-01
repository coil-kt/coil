package coil3.request

import coil3.Extras
import coil3.PlatformContext
import coil3.annotation.Data
import coil3.decode.Decoder
import coil3.fetch.Fetcher
import coil3.size.Scale
import coil3.size.Size
import coil3.util.defaultFileSystem
import okio.FileSystem

/**
 * A set of configuration options for fetching and decoding an image.
 *
 * [Fetcher]s and [Decoder]s should respect these options as best as possible.
 */
@Data
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
     * 'true' if the output image does not need to fit/fill the target's dimensions exactly.
     * For instance, if 'true' `BitmapFactoryDecoder` will not decode an image at a larger size
     * than its source dimensions as an optimization.
     */
    val allowInexactSize: Boolean = false,

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
        allowInexactSize: Boolean = this.allowInexactSize,
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
        allowInexactSize = allowInexactSize,
        diskCacheKey = diskCacheKey,
        fileSystem = fileSystem,
        memoryCachePolicy = memoryCachePolicy,
        diskCachePolicy = diskCachePolicy,
        networkCachePolicy = networkCachePolicy,
        extras = extras,
    )
}
