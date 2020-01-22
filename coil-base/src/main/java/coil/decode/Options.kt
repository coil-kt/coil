package coil.decode

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
 * @param config The requested config for any [Bitmap]s.
 * @param colorSpace The preferred color space for any [Bitmap]s.
 *  If null, components should typically default to [ColorSpace.Rgb].
 * @param scale The scaling algorithm for how to fit the source image's dimensions into the target's dimensions.
 * @param allowInexactSize True if the output image does not need to fit/fill the target's dimensions exactly. For instance,
 *  if true [BitmapFactoryDecoder] will not decode an image at a larger size than its source dimensions as an optimization.
 * @param allowRgb565 True if a component is allowed to use [Bitmap.Config.RGB_565] as an optimization.
 * @param headers The header fields to use for any network requests.
 * @param parameters A map of custom parameters. These are used to pass custom data to a component.
 * @param networkCachePolicy Determines if this request is allowed to read from the network.
 * @param diskCachePolicy Determines if this request is allowed to read/write from/to disk.
 */
data class Options(
    val config: Bitmap.Config,
    val colorSpace: ColorSpace?,
    val scale: Scale,
    val allowInexactSize: Boolean,
    val allowRgb565: Boolean,
    val headers: Headers,
    val parameters: Parameters,
    val networkCachePolicy: CachePolicy,
    val diskCachePolicy: CachePolicy
)
