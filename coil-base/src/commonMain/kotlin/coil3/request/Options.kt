package coil3.request

import coil3.PlatformContext
import coil3.Extras
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
data class Options(
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
     * TODO
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
     * TODO
     */
    val extras: Extras = Extras.EMPTY,
)
