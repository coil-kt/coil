package coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import coil.decode.BitmapFactoryDecoder
import coil.decode.ExifOrientationPolicy
import coil.util.DEFAULT_BITMAP_CONFIG
import coil.util.internalExtraKeyOf

/**
 * Create a new [ImageLoader] without configuration.
 */
fun ImageLoader(context: Context): ImageLoader {
    return ImageLoader(context.asPlatformContext())
}

/**
 * Sets the maximum number of parallel [BitmapFactory] decode operations at once.
 *
 * Increasing this number will allow more parallel [BitmapFactory] decode operations,
 * however it can result in worse UI performance.
 *
 * Default: [bitmapFactoryMaxParallelismDefault]
 */
fun ImageLoader.Builder.bitmapFactoryMaxParallelism(maxParallelism: Int) = apply {
    require(maxParallelism > 0) { "maxParallelism must be > 0." }
    extra(bitmapFactoryMaxParallelismKey, maxParallelism)
}

private val bitmapFactoryMaxParallelismKey = internalExtraKeyOf("bitmapFactoryMaxParallelism")
private const val bitmapFactoryMaxParallelismDefault = BitmapFactoryDecoder.DEFAULT_MAX_PARALLELISM
internal val RealImageLoader.Options.bitmapFactoryMaxParallelism
    get() = extras.get(bitmapFactoryMaxParallelismKey) ?: bitmapFactoryMaxParallelismDefault

/**
 * Sets the policy for handling the EXIF orientation flag for images decoded by
 * [BitmapFactoryDecoder].
 *
 * Default: [bitmapFactoryExifOrientationPolicyDefault]
 */
fun ImageLoader.Builder.bitmapFactoryExifOrientationPolicy(policy: ExifOrientationPolicy) = apply {
    extra(bitmapFactoryExifOrientationPolicyKey, policy)
}

private val bitmapFactoryExifOrientationPolicyKey = internalExtraKeyOf("bitmapFactoryExifOrientationPolicy")
private val bitmapFactoryExifOrientationPolicyDefault = ExifOrientationPolicy.RESPECT_PERFORMANCE
internal val RealImageLoader.Options.bitmapFactoryExifOrientationPolicy
    get() = extras.get(bitmapFactoryExifOrientationPolicyKey) ?: bitmapFactoryExifOrientationPolicyDefault

/**
 * Allow the use of [Bitmap.Config.HARDWARE].
 *
 * If false, any use of [Bitmap.Config.HARDWARE] will be treated as
 * [Bitmap.Config.ARGB_8888].
 *
 * NOTE: Setting this to false this will reduce performance on API 26 and above. Only
 * disable this if necessary.
 *
 * Default: [allowHardwareDefault]
 */
fun ImageLoader.Builder.allowHardware(enable: Boolean) = apply {
    extra(allowHardwareKey, enable)
}

private val allowHardwareKey = internalExtraKeyOf("allowHardware")
private const val allowHardwareDefault = true
internal val RealImageLoader.Options.allowHardware
    get() = extras.get(allowHardwareKey) ?: allowHardwareDefault

/**
 * Allow automatically using [Bitmap.Config.RGB_565] when an image is guaranteed to not
 * have alpha.
 *
 * This will reduce the visual quality of the image, but will also reduce memory usage.
 *
 * Prefer only enabling this for low memory and resource constrained devices.
 *
 * Default: [allowRgb565Default]
 */
fun ImageLoader.Builder.allowRgb565(enable: Boolean) = apply {
    extra(allowRgb565Key, enable)
}

private val allowRgb565Key = internalExtraKeyOf("allowRgb565")
private const val allowRgb565Default = false
internal val RealImageLoader.Options.allowRgb565
    get() = extras.get(allowRgb565Key) ?: allowRgb565Default

/**
 * Set the preferred [Bitmap.Config].
 *
 * This is not guaranteed and a different config may be used in some situations.
 *
 * Default: [bitmapConfigDefault]
 */
fun ImageLoader.Builder.bitmapConfig(bitmapConfig: Bitmap.Config) = apply {
    extra(bitmapConfigKey, bitmapConfig)
}

private val bitmapConfigKey = internalExtraKeyOf("bitmapConfig")
private val bitmapConfigDefault = DEFAULT_BITMAP_CONFIG
internal val RealImageLoader.Options.bitmapConfig
    get() = extras.get(bitmapConfigKey) ?: bitmapConfigDefault
