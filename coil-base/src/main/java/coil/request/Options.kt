package coil.request

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.os.Build.VERSION.SDK_INT
import coil.decode.BitmapFactoryDecoder
import coil.decode.Decoder
import coil.fetch.Fetcher
import coil.size.Scale
import coil.size.Size
import coil.util.EMPTY_HEADERS
import coil.util.NULL_COLOR_SPACE
import okhttp3.Headers

/**
 * A set of configuration options for fetching and decoding an image.
 * [Fetcher]s and [Decoder]s should respect these options as best as possible.
 */
class Options(
    /**
     * The [Context] used to execute this request.
     */
    val context: Context,

    /**
     * The requested config for any [Bitmap]s.
     */
    val config: Bitmap.Config = Bitmap.Config.ARGB_8888,

    /**
     * The preferred color space for any [Bitmap]s.
     * If 'null', components should typically default to [ColorSpace.Rgb].
     */
    val colorSpace: ColorSpace? = NULL_COLOR_SPACE,

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
     * For instance, if 'true' [BitmapFactoryDecoder] will not decode an image at a larger size
     * than its source dimensions as an optimization.
     */
    val allowInexactSize: Boolean = false,

    /**
     * 'true' if a component is allowed to use [Bitmap.Config.RGB_565] as an optimization.
     * As RGB_565 does not have an alpha channel, components should only use RGB_565 if the
     * image is guaranteed to not use alpha.
     */
    val allowRgb565: Boolean = false,

    /**
     * 'true' if the color (RGB) channels of the decoded image should be pre-multiplied by the
     * alpha channel. The default behavior is to enable pre-multiplication but in some environments
     * it can be necessary to disable this feature to leave the source pixels unmodified.
     */
    val premultipliedAlpha: Boolean = true,

    /**
     * The cache key to use when persisting images to the disk cache or 'null' if the component can
     * compute its own.
     */
    val diskCacheKey: String? = null,

    /**
     * The tags to use for any network requests.
     */
    val tags: List<Any>? = null,

    /**
     * The header fields to use for any network requests.
     */
    val headers: Headers = EMPTY_HEADERS,

    /**
     * A map of custom parameters. These are used to pass custom data to a component.
     */
    val parameters: Parameters = Parameters.EMPTY,

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
) {

    fun copy(
        context: Context = this.context,
        config: Bitmap.Config = this.config,
        colorSpace: ColorSpace? = this.colorSpace,
        size: Size = this.size,
        scale: Scale = this.scale,
        allowInexactSize: Boolean = this.allowInexactSize,
        allowRgb565: Boolean = this.allowRgb565,
        premultipliedAlpha: Boolean = this.premultipliedAlpha,
        diskCacheKey: String? = this.diskCacheKey,
        tags: List<Any>? = this.tags,
        headers: Headers = this.headers,
        parameters: Parameters = this.parameters,
        memoryCachePolicy: CachePolicy = this.memoryCachePolicy,
        diskCachePolicy: CachePolicy = this.diskCachePolicy,
        networkCachePolicy: CachePolicy = this.networkCachePolicy,
    ) = Options(
        context = context,
        config = config,
        colorSpace = colorSpace,
        size = size,
        scale = scale,
        allowInexactSize = allowInexactSize,
        allowRgb565 = allowRgb565,
        premultipliedAlpha = premultipliedAlpha,
        diskCacheKey = diskCacheKey,
        tags = tags,
        headers = headers,
        parameters = parameters,
        memoryCachePolicy = memoryCachePolicy,
        diskCachePolicy = diskCachePolicy,
        networkCachePolicy = networkCachePolicy,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is Options &&
            context == other.context &&
            config == other.config &&
            (SDK_INT < 26 || colorSpace == other.colorSpace) &&
            size == other.size &&
            scale == other.scale &&
            allowInexactSize == other.allowInexactSize &&
            allowRgb565 == other.allowRgb565 &&
            premultipliedAlpha == other.premultipliedAlpha &&
            diskCacheKey == other.diskCacheKey &&
            tags == other.tags &&
            headers == other.headers &&
            parameters == other.parameters &&
            memoryCachePolicy == other.memoryCachePolicy &&
            diskCachePolicy == other.diskCachePolicy &&
            networkCachePolicy == other.networkCachePolicy
    }

    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + config.hashCode()
        result = 31 * result + (colorSpace?.hashCode() ?: 0)
        result = 31 * result + size.hashCode()
        result = 31 * result + scale.hashCode()
        result = 31 * result + allowInexactSize.hashCode()
        result = 31 * result + allowRgb565.hashCode()
        result = 31 * result + premultipliedAlpha.hashCode()
        result = 31 * result + (diskCacheKey?.hashCode() ?: 0)
        result = 31 * result + (tags?.hashCode() ?: 0)
        result = 31 * result + headers.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + memoryCachePolicy.hashCode()
        result = 31 * result + diskCachePolicy.hashCode()
        result = 31 * result + networkCachePolicy.hashCode()
        return result
    }
}
