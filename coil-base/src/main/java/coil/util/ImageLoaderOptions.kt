package coil.util

import coil.ImageLoader
import coil.RealImageLoader
import coil.decode.BitmapFactoryDecoder.Companion.DEFAULT_MAX_PARALLELISM

/**
 * Private configuration options used by [RealImageLoader].
 *
 * @see ImageLoader.Builder
 */
internal class ImageLoaderOptions(
    val addLastModifiedToFileCacheKey: Boolean = true,
    val networkObserverEnabled: Boolean = true,
    val bitmapFactoryMaxParallelism: Int = DEFAULT_MAX_PARALLELISM,
) {

    fun copy(
        addLastModifiedToFileCacheKey: Boolean = this.addLastModifiedToFileCacheKey,
        networkObserverEnabled: Boolean = this.networkObserverEnabled,
        bitmapFactoryMaxParallelism: Int = this.bitmapFactoryMaxParallelism,
    ) = ImageLoaderOptions(addLastModifiedToFileCacheKey, networkObserverEnabled, bitmapFactoryMaxParallelism)
}
