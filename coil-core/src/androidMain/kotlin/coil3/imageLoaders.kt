package coil3

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import coil3.decode.BitmapFactoryDecoder
import coil3.decode.BitmapFactoryDecoder.Companion.DEFAULT_MAX_PARALLELISM
import coil3.decode.ExifOrientationPolicy
import coil3.decode.ExifOrientationPolicy.RESPECT_PERFORMANCE

// region bitmapFactoryMaxParallelism

/**
 * Sets the maximum number of parallel [BitmapFactory] or [ImageDecoder] decode operations at once.
 *
 * Increasing this number will allow more parallel decode operations, however it can result in
 * worse UI performance.
 */
fun ImageLoader.Builder.bitmapFactoryMaxParallelism(maxParallelism: Int) = apply {
    require(maxParallelism > 0) { "maxParallelism must be > 0." }
    extras[bitmapFactoryMaxParallelismKey] = maxParallelism
}

internal val RealImageLoader.Options.bitmapFactoryMaxParallelism: Int
    get() = defaults.extras.getOrDefault(bitmapFactoryMaxParallelismKey)

private val bitmapFactoryMaxParallelismKey = Extras.Key(default = DEFAULT_MAX_PARALLELISM)

// endregion
// region bitmapFactoryExifOrientationPolicy

/**
 * Sets the policy for handling the EXIF orientation flag for images decoded by
 * [BitmapFactoryDecoder].
 */
fun ImageLoader.Builder.bitmapFactoryExifOrientationPolicy(policy: ExifOrientationPolicy) = apply {
    extras[bitmapFactoryExifOrientationPolicyKey] = policy
}

internal val RealImageLoader.Options.bitmapFactoryExifOrientationPolicy: ExifOrientationPolicy
    get() = defaults.extras.getOrDefault(bitmapFactoryExifOrientationPolicyKey)

private val bitmapFactoryExifOrientationPolicyKey = Extras.Key(default = RESPECT_PERFORMANCE)

// endregion
