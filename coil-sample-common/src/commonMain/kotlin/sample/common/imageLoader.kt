package sample.common

import coil.ComponentRegistry
import coil.EventListener
import coil.ImageLoader
import coil.PlatformContext
import coil.disk.DiskCache
import coil.fetch.NetworkFetcher
import coil.memory.MemoryCache
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.crossfade
import coil.util.DebugLogger
import okio.FileSystem

fun newImageLoader(
    context: PlatformContext,
    debug: Boolean,
): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            add(NetworkFetcher.Factory())
            addPlatformComponents()
        }
        .memoryCache {
            MemoryCache.Builder()
                // Set the max size to 25% of the app's available memory.
                .maxSizePercent(context, percent = 0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
                .maxSizeBytes(512L * 1024 * 1024) // 512MB
                .build()
        }
        // Show a short crossfade when loading images asynchronously.
        .crossfade(true)
        // Enable logging if this is a debug build.
        .apply {
            if (debug) {
                logger(DebugLogger())
            }
        }
        .eventListener(object : EventListener() {
            override fun onError(request: ImageRequest, result: ErrorResult) {
                super.onError(request, result)
            }
        })
        .build()
}

internal expect fun ComponentRegistry.Builder.addPlatformComponents()
