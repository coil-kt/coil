@file:Suppress("unused")

package coil.sample

import android.app.Application
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import coil.memory.MemoryCache
import coil.util.DebugLogger
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.io.File

class Application : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                // GIFs
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                // SVGs
                add(SvgDecoder.Factory())
                // Video frames
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true) // Show a short crossfade when loading images asynchronously.
            .memoryCache(
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Set the max size to 25% of the app's memory.
                    .build()
            )
            .okHttpClient {
                // Create a disk cache with "unlimited" size. Don't do this in production.
                // To create the an optimized Coil disk cache, use CoilUtils.createDiskCache(context).
                val cacheDirectory = File(filesDir, "image_cache").apply { mkdirs() }
                val diskCache = Cache(cacheDirectory, Long.MAX_VALUE)

                // Don't limit concurrent network requests by host.
                val dispatcher = Dispatcher().apply { maxRequestsPerHost = maxRequests }

                // Rewrite the Cache-Control header to cache all responses for a year.
                val cacheControlInterceptor = ResponseHeaderInterceptor(
                    name = "Cache-Control",
                    value = "max-age=31536000,public"
                )

                // Lazily create the OkHttpClient that is used for network operations.
                OkHttpClient.Builder()
                    .cache(diskCache)
                    .dispatcher(dispatcher)
                    .addNetworkInterceptor(cacheControlInterceptor)
                    .build()
            }
            .apply {
                // Enable logging to the standard Android log if this is a debug build.
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger(Log.VERBOSE))
                }
            }
            .build()
    }
}
