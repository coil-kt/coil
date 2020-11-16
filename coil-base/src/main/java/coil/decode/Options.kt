@file:Suppress("unused")

package coil.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import coil.fetch.Fetcher
import coil.request.CachePolicy
import coil.request.Parameters
import coil.size.Scale
import coil.util.EMPTY_HEADERS
import coil.util.NULL_COLOR_SPACE
import okhttp3.Headers

/**
 * A set of configuration options for fetching and decoding an image.
 *
 * [Fetcher]s and [Decoder]s should respect these options as best as possible.
 *
 * @param context The [Context] used to execute this request.
 * @param config The requested config for any [Bitmap]s.
 * @param colorSpace The preferred color space for any [Bitmap]s.
 *  If null, components should typically default to [ColorSpace.Rgb].
 * @param scale The scaling algorithm for how to fit the source image's dimensions into the target's dimensions.
 * @param allowInexactSize True if the output image does not need to fit/fill the target's dimensions exactly. For instance,
 *  if true [BitmapFactoryDecoder] will not decode an image at a larger size than its source dimensions as an optimization.
 * @param allowRgb565 True if a component is allowed to use [Bitmap.Config.RGB_565] as an optimization. As RGB_565 does
 *  not have an alpha channel, components should only use RGB_565 if the image is guaranteed to not use alpha.
 * @param premultipliedAlpha True if the color (RGB) channels of the decoded image should be pre-multiplied by the
 *  alpha channel. The default behavior is to enable pre-multiplication but in some environments it can be necessary
 *  to disable this feature to leave the source pixels unmodified.
 * @param headers The header fields to use for any network requests.
 * @param parameters A map of custom parameters. These are used to pass custom data to a component.
 * @param memoryCachePolicy Determines if this request is allowed to read/write from/to memory.
 * @param diskCachePolicy Determines if this request is allowed to read/write from/to disk.
 * @param networkCachePolicy Determines if this request is allowed to read from the network.
 */
class Options(
    val context: Context,
    val config: Bitmap.Config = Bitmap.Config.ARGB_8888,
    val colorSpace: ColorSpace? = NULL_COLOR_SPACE,
    val scale: Scale = Scale.FIT,
    val allowInexactSize: Boolean = false,
    val allowRgb565: Boolean = false,
    val premultipliedAlpha: Boolean = true,
    val headers: Headers = EMPTY_HEADERS,
    val parameters: Parameters = Parameters.EMPTY,
    val memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,
    val diskCachePolicy: CachePolicy = CachePolicy.ENABLED,
    val networkCachePolicy: CachePolicy = CachePolicy.ENABLED
) {

    fun copy(
        context: Context = this.context,
        config: Bitmap.Config = this.config,
        colorSpace: ColorSpace? = this.colorSpace,
        scale: Scale = this.scale,
        allowInexactSize: Boolean = this.allowInexactSize,
        allowRgb565: Boolean = this.allowRgb565,
        premultipliedAlpha: Boolean = this.premultipliedAlpha,
        headers: Headers = this.headers,
        parameters: Parameters = this.parameters,
        memoryCachePolicy: CachePolicy = this.memoryCachePolicy,
        diskCachePolicy: CachePolicy = this.diskCachePolicy,
        networkCachePolicy: CachePolicy = this.networkCachePolicy
    ) = Options(context, config, colorSpace, scale, allowInexactSize, allowRgb565, premultipliedAlpha,
        headers, parameters, memoryCachePolicy, diskCachePolicy, networkCachePolicy)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is Options &&
            context == other.context &&
            config == other.config &&
            colorSpace == other.colorSpace &&
            scale == other.scale &&
            allowInexactSize == other.allowInexactSize &&
            allowRgb565 == other.allowRgb565 &&
            premultipliedAlpha == other.premultipliedAlpha &&
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
        result = 31 * result + scale.hashCode()
        result = 31 * result + allowInexactSize.hashCode()
        result = 31 * result + allowRgb565.hashCode()
        result = 31 * result + premultipliedAlpha.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + memoryCachePolicy.hashCode()
        result = 31 * result + diskCachePolicy.hashCode()
        result = 31 * result + networkCachePolicy.hashCode()
        return result
    }

    override fun toString(): String {
        return "Options(context=$context, config=$config, colorSpace=$colorSpace, scale=$scale, " +
            "allowInexactSize=$allowInexactSize, allowRgb565=$allowRgb565, premultipliedAlpha=$premultipliedAlpha, " +
            "headers=$headers, parameters=$parameters, memoryCachePolicy=$memoryCachePolicy, " +
            "diskCachePolicy=$diskCachePolicy, networkCachePolicy=$networkCachePolicy)"
    }

    @Deprecated(message = "Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    constructor(
        context: Context,
        config: Bitmap.Config,
        colorSpace: ColorSpace?,
        scale: Scale,
        allowInexactSize: Boolean,
        allowRgb565: Boolean,
        headers: Headers,
        parameters: Parameters,
        memoryCachePolicy: CachePolicy,
        diskCachePolicy: CachePolicy,
        networkCachePolicy: CachePolicy
    ) : this(context = context, config = config, colorSpace = colorSpace, scale = scale, allowInexactSize = allowInexactSize,
        allowRgb565 = allowRgb565, headers = headers, parameters = parameters, memoryCachePolicy = memoryCachePolicy,
        diskCachePolicy = diskCachePolicy, networkCachePolicy = networkCachePolicy)

    @Deprecated(message = "Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    fun copy(
        context: Context = this.context,
        config: Bitmap.Config = this.config,
        colorSpace: ColorSpace? = this.colorSpace,
        scale: Scale = this.scale,
        allowInexactSize: Boolean = this.allowInexactSize,
        allowRgb565: Boolean = this.allowRgb565,
        headers: Headers = this.headers,
        parameters: Parameters = this.parameters,
        memoryCachePolicy: CachePolicy = this.memoryCachePolicy,
        diskCachePolicy: CachePolicy = this.diskCachePolicy,
        networkCachePolicy: CachePolicy = this.networkCachePolicy
    ) = copy(context, config, colorSpace, scale, allowInexactSize, allowRgb565, premultipliedAlpha, headers, parameters,
        memoryCachePolicy, diskCachePolicy, networkCachePolicy)
}
