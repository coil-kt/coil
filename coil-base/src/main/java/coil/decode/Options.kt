@file:Suppress("unused")

package coil.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import androidx.annotation.RequiresApi
import coil.decode.Options.Builder
import coil.fetch.Fetcher
import coil.request.CachePolicy
import coil.request.Parameters
import coil.size.Scale
import coil.util.EMPTY_HEADERS
import okhttp3.Headers

/**
 * A set of configuration options for fetching and decoding an image.
 * [Fetcher]s and [Decoder]s should respect these options as best as possible.
 */
class Options private constructor(
    val context: Context,

    /** @see Builder.config */
    val config: Bitmap.Config,

    /** @see Builder.colorSpace */
    val colorSpace: ColorSpace?,

    /** @see Builder.scale */
    val scale: Scale,

    /** @see Builder.allowInexactSize */
    val allowInexactSize: Boolean,

    /** @see Builder.allowRgb565 */
    val allowRgb565: Boolean,

    /** @see Builder.premultipliedAlpha */
    val premultipliedAlpha: Boolean,

    /** @see Builder.headers */
    val headers: Headers,

    /** @see Builder.parameters */
    val parameters: Parameters,

    /** @see Builder.memoryCachePolicy */
    val memoryCachePolicy: CachePolicy,

    /** @see Builder.diskCachePolicy */
    val diskCachePolicy: CachePolicy,

    /** @see Builder.networkCachePolicy */
    val networkCachePolicy: CachePolicy
) {

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

    class Builder(private val context: Context) {

        private var config = Bitmap.Config.ARGB_8888
        private var colorSpace: ColorSpace? = null
        private var scale = Scale.FIT
        private var allowInexactSize = false
        private var allowRgb565 = false
        private var premultipliedAlpha = true
        private var headers = EMPTY_HEADERS
        private var parameters = Parameters.EMPTY
        private var memoryCachePolicy = CachePolicy.ENABLED
        private var diskCachePolicy = CachePolicy.ENABLED
        private var networkCachePolicy = CachePolicy.ENABLED

        /**
         * The requested config for any [Bitmap]s.
         */
        fun config(config: Bitmap.Config) = apply {
            this.config = config
        }

        /**
         * The preferred color space for any [Bitmap]s.
         *
         * If null, components should typically default to [ColorSpace.Rgb].
         */
        @RequiresApi(26)
        fun colorSpace(colorSpace: ColorSpace) = apply {
            this.colorSpace = colorSpace
        }

        /**
         * The scaling algorithm for how to fit the source image's dimensions into the target's dimensions.
         */
        fun scale(scale: Scale) = apply {
            this.scale = scale
        }

        /**
         * True if the output image does not need to fit/fill the target's dimensions exactly.
         *
         * For instance, if true [BitmapFactoryDecoder] will not decode an image at a larger size than its
         * source dimensions as an optimization.
         */
        fun allowInexactSize(enable: Boolean) = apply {
            this.allowInexactSize = enable
        }

        /**
         * True if a component is allowed to use [Bitmap.Config.RGB_565] as an optimization.
         *
         * As RGB_565 does not have an alpha channel, components should only use RGB_565 if the image
         * is guaranteed to not use alpha.
         */
        fun allowRgb565(enable: Boolean) = apply {
            this.allowRgb565 = enable
        }

        /**
         * True if the color (RGB) channels of the decoded image should be pre-multiplied by the alpha channel.
         *
         * The default behavior is to enable pre-multiplication but in some environments it can be necessary
         * to disable this feature to leave the source pixels unmodified.
         */
        fun premultipliedAlpha(enable: Boolean) = apply {
            this.premultipliedAlpha = enable
        }

        /**
         * The header fields to use for any network requests.
         */
        fun headers(headers: Headers) = apply {
            this.headers = headers
        }

        /**
         * A map of custom parameters. These are used to pass custom data to a component.
         */
        fun parameters(parameters: Parameters) = apply {
            this.parameters = parameters
        }

        /**
         * Determines if this request is allowed to read/write from/to memory.
         */
        fun memoryCachePolicy(policy: CachePolicy) = apply {
            this.memoryCachePolicy = policy
        }

        /**
         * Determines if this request is allowed to read/write from/to disk.
         */
        fun diskCachePolicy(policy: CachePolicy) = apply {
            this.diskCachePolicy = policy
        }

        /**
         * Determines if this request is allowed to read from the network.
         */
        fun networkCachePolicy(policy: CachePolicy) = apply {
            this.networkCachePolicy = policy
        }

        /**
         * Create a new [Options].
         */
        fun build(): Options {
            return Options(context, config, colorSpace, scale, allowInexactSize, allowRgb565, premultipliedAlpha,
                headers, parameters, memoryCachePolicy, diskCachePolicy, networkCachePolicy)
        }
    }

    @Deprecated(message = "Use `Options.Builder` to create new instances.", level = DeprecationLevel.WARNING)
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
    ) : this(context, config, colorSpace, scale, allowInexactSize, allowRgb565, false, headers,
        parameters, memoryCachePolicy, diskCachePolicy, networkCachePolicy)

    @Deprecated(message = "Use `newBuilder` to create new instances.", level = DeprecationLevel.WARNING)
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
    ) = Options(context, config, colorSpace, scale, allowInexactSize, allowRgb565, premultipliedAlpha, headers,
        parameters, memoryCachePolicy, diskCachePolicy, networkCachePolicy)
}
