package coil.util

import coil.ImageLoader
import coil.RealImageLoader
import coil.decode.BitmapFactoryDecoder.Companion.DEFAULT_MAX_PARALLELISM
import coil.decode.ExifOrientationPolicy

/**
 * Private configuration options used by [RealImageLoader].
 *
 * @see ImageLoader.Builder
 */
internal data class ImageLoaderOptions(
    val addLastModifiedToFileCacheKey: Boolean = true,
    val networkObserverEnabled: Boolean = true,
    val respectCacheHeaders: Boolean = true,
    val bitmapFactoryMaxParallelism: Int = DEFAULT_MAX_PARALLELISM,
    val bitmapFactoryExifOrientationPolicy: ExifOrientationPolicy = ExifOrientationPolicy.RESPECT_PERFORMANCE
)
