package coil.decode

import android.graphics.Bitmap
import android.graphics.ColorSpace
import coil.fetch.Fetcher
import coil.request.CachePolicy
import coil.request.Parameters
import coil.size.Scale
import okhttp3.Headers

/**
 * A set of configuration options for loading and decoding an image.
 *
 * [Fetcher]s and [Decoder]s should respect these options as best as possible.
 *
 * @param config The requested config for any [Bitmap]s.
 * @param colorSpace The preferred color space for any [Bitmap]s.
 * @param scale Determines if the image should be loaded to fit or fill the target's dimensions.
 * @param requireExactSize True if the image can be loaded at a bigger or smaller size than the requested dimensions.
 *  This will be true if the target supports scaling the image to its required dimensions.
 * @param allowRgb565 True if the [Fetcher] is allowed to use [Bitmap.Config.RGB_565] as an optimization.
 * @param headers The headers for any network operations.
 * @param parameters A map of custom parameters. These are used to pass custom data to [Fetcher]s and [Decoder]s.
 * @param networkCachePolicy Used to determine if this request is allowed to read from the network.
 * @param diskCachePolicy Used to determine if this request is allowed to read/write from/to disk.
 */
data class Options(
    val config: Bitmap.Config,
    val colorSpace: ColorSpace?,
    val scale: Scale,
    val allowRgb565: Boolean,
    val requireExactSize: Boolean,
    val headers: Headers,
    val parameters: Parameters,
    val networkCachePolicy: CachePolicy,
    val diskCachePolicy: CachePolicy
)
