package coil

import android.graphics.BitmapFactory
import coil.decode.BitmapFactoryDecoder
import coil.decode.ExifOrientationPolicy
import coil.util.internalExtraKeyOf

// bitmapFactoryMaxParallelism

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

internal val RealImageLoader.Options.bitmapFactoryMaxParallelism
    get() = extras.get(bitmapFactoryMaxParallelismKey) ?: bitmapFactoryMaxParallelismDefault

private val bitmapFactoryMaxParallelismKey = internalExtraKeyOf("bitmapFactoryMaxParallelism")
private const val bitmapFactoryMaxParallelismDefault = BitmapFactoryDecoder.DEFAULT_MAX_PARALLELISM

// bitmapFactoryExifOrientationPolicy

/**
 * Sets the policy for handling the EXIF orientation flag for images decoded by
 * [BitmapFactoryDecoder].
 *
 * Default: [bitmapFactoryExifOrientationPolicyDefault]
 */
fun ImageLoader.Builder.bitmapFactoryExifOrientationPolicy(policy: ExifOrientationPolicy) = apply {
    extra(bitmapFactoryExifOrientationPolicyKey, policy)
}

internal val RealImageLoader.Options.bitmapFactoryExifOrientationPolicy
    get() = extras.get(bitmapFactoryExifOrientationPolicyKey) ?: bitmapFactoryExifOrientationPolicyDefault

private val bitmapFactoryExifOrientationPolicyKey = internalExtraKeyOf("bitmapFactoryExifOrientationPolicy")
private val bitmapFactoryExifOrientationPolicyDefault = ExifOrientationPolicy.RESPECT_PERFORMANCE
