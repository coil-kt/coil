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
internal class ImageLoaderOptions(
    val addLastModifiedToFileCacheKey: Boolean = true,
    val networkObserverEnabled: Boolean = true,
    val respectCacheHeaders: Boolean = true,
    val bitmapFactoryMaxParallelism: Int = DEFAULT_MAX_PARALLELISM,
    val bitmapFactoryExifOrientationPolicy: ExifOrientationPolicy = ExifOrientationPolicy.RESPECT_PERFORMANCE
) {

    fun copy(
        addLastModifiedToFileCacheKey: Boolean = this.addLastModifiedToFileCacheKey,
        networkObserverEnabled: Boolean = this.networkObserverEnabled,
        respectCacheHeaders: Boolean = this.respectCacheHeaders,
        bitmapFactoryMaxParallelism: Int = this.bitmapFactoryMaxParallelism,
        bitmapFactoryExifOrientationPolicy: ExifOrientationPolicy = this.bitmapFactoryExifOrientationPolicy,
    ) = ImageLoaderOptions(
        addLastModifiedToFileCacheKey,
        networkObserverEnabled,
        respectCacheHeaders,
        bitmapFactoryMaxParallelism,
        bitmapFactoryExifOrientationPolicy
    )
}
