package coil3

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import androidx.lifecycle.Lifecycle
import coil3.annotation.ExperimentalCoilApi
import coil3.decode.BitmapFactoryDecoder
import coil3.decode.BitmapFactoryDecoder.Companion.DEFAULT_MAX_PARALLELISM
import coil3.decode.Decoder
import coil3.decode.ExifOrientationStrategy
import coil3.decode.ExifOrientationStrategy.Companion.RESPECT_PERFORMANCE
import coil3.memory.MemoryCache

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
// region bitmapFactoryExifOrientationStrategy

/**
 * Sets the strategy for handling the EXIF orientation flag for images decoded by
 * [BitmapFactoryDecoder].
 */
fun ImageLoader.Builder.bitmapFactoryExifOrientationStrategy(strategy: ExifOrientationStrategy) = apply {
    extras[bitmapFactoryExifOrientationStrategyKey] = strategy
}

internal val RealImageLoader.Options.bitmapFactoryExifOrientationStrategy: ExifOrientationStrategy
    get() = defaults.extras.getOrDefault(bitmapFactoryExifOrientationStrategyKey)

private val bitmapFactoryExifOrientationStrategyKey = Extras.Key(default = RESPECT_PERFORMANCE)

// endregion
// region imageDecoderEnabled

/**
 * Enables using [ImageDecoder] as this image loader's main [Decoder] on API 29 and above.
 * If false, [BitmapFactory] is used on all API levels.
 */
fun ImageLoader.Builder.imageDecoderEnabled(enabled: Boolean) = apply {
    extras[imageDecoderEnabledKey] = enabled
}

internal val RealImageLoader.Options.imageDecoderEnabled: Boolean
    get() = defaults.extras.getOrDefault(imageDecoderEnabledKey)

private val imageDecoderEnabledKey = Extras.Key(default = true)

// endregion
// region trimMemoryCacheOnBackground

/**
 * Reduces the memory cache's [MemoryCache.maxSize] to a [percent] of [MemoryCache.initialMaxSize]
 * while the app is in the background (i.e. while its process lifecycle state is not at least
 * [Lifecycle.State.STARTED]).
 *
 * This option helps proactively free up memory while the app's UI isn't visible. Lower values
 * free up more memory. Setting this option to 1.0 (the default) will not reduce the memory cache's
 * max size when the app is backgrounded. Setting this option to 0.0 will free all strong references
 * to the images in the memory cache when the app is backgrounded.
 */
@ExperimentalCoilApi
fun ImageLoader.Builder.memoryCacheMaxSizePercentWhileInBackground(percent: Double) = apply {
    require(percent in 0.0..1.0) { "percent must be in the range [0.0, 1.0]." }
    extras[memoryCacheMaxSizePercentWhileInBackgroundKey] = percent
}

internal val RealImageLoader.Options.memoryCacheMaxSizePercentWhileInBackground: Double
    get() = defaults.extras.getOrDefault(memoryCacheMaxSizePercentWhileInBackgroundKey)

private val memoryCacheMaxSizePercentWhileInBackgroundKey = Extras.Key(default = 1.0)

// endregion
