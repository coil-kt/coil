package coil.util

import coil.ImageLoader
import coil.RealImageLoader

/**
 * Private configuration options used by [RealImageLoader].
 *
 * @see ImageLoader.Builder
 */
internal data class ImageLoaderOptions(
    val addLastModifiedToFileCacheKey: Boolean = true,
    val launchInterceptorChainOnMainThread: Boolean = true
)
