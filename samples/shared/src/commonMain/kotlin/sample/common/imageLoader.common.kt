package sample.common

import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.decode.SvgDecoder
import coil3.disk.DiskCache
import coil3.fetch.NetworkFetcher
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.util.DebugLogger

fun newImageLoader(
    context: PlatformContext,
    debug: Boolean = false,
): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            add(NetworkFetcher.Factory())
            // SVGs
            add(SvgDecoder.Factory())
            addPlatformComponents()
        }
        .memoryCache {
            MemoryCache.Builder()
                // Set the max size to 25% of the app's available memory.
                .maxSizePercent(context, percent = 0.25)
                .build()
        }
        .diskCache {
            newDiskCache()
        }
        // Show a short crossfade when loading images asynchronously.
        .crossfade(true)
        // Enable logging if this is a debug build.
        .apply {
            if (debug) {
                logger(DebugLogger())
            }
        }
        .build()
}

internal expect fun newDiskCache(): DiskCache?

internal expect fun ComponentRegistry.Builder.addPlatformComponents()
