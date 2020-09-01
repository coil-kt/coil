package coil.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import coil.fetch.Fetcher
import coil.request.CachePolicy
import coil.request.Parameters
import coil.size.Scale
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
 * @param headers The header fields to use for any network requests.
 * @param parameters A map of custom parameters. These are used to pass custom data to a component.
 * @param memoryCachePolicy Determines if this request is allowed to read/write from/to memory.
 * @param diskCachePolicy Determines if this request is allowed to read/write from/to disk.
 * @param networkCachePolicy Determines if this request is allowed to read from the network.
 */
class Options(
    val context: Context,
    val config: Bitmap.Config,
    val colorSpace: ColorSpace?,
    val scale: Scale,
    val allowInexactSize: Boolean,
    val allowRgb565: Boolean,
    val headers: Headers,
    val parameters: Parameters,
    val memoryCachePolicy: CachePolicy,
    val diskCachePolicy: CachePolicy,
    val networkCachePolicy: CachePolicy
) {

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
    ) = Options(context, config, colorSpace, scale, allowInexactSize, allowRgb565, headers, parameters,
        memoryCachePolicy, diskCachePolicy, networkCachePolicy)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is Options &&
            context == other.context &&
            config == other.config &&
            colorSpace == other.colorSpace &&
            scale == other.scale &&
            allowInexactSize == other.allowInexactSize &&
            allowRgb565 == other.allowRgb565 &&
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
        result = 31 * result + headers.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + memoryCachePolicy.hashCode()
        result = 31 * result + diskCachePolicy.hashCode()
        result = 31 * result + networkCachePolicy.hashCode()
        return result
    }

    override fun toString(): String {
        return "Options(context=$context, config=$config, colorSpace=$colorSpace, scale=$scale, " +
            "allowInexactSize=$allowInexactSize, allowRgb565=$allowRgb565, headers=$headers, " +
            "parameters=$parameters, memoryCachePolicy=$memoryCachePolicy, diskCachePolicy=$diskCachePolicy, " +
            "networkCachePolicy=$networkCachePolicy)"
    }
}
