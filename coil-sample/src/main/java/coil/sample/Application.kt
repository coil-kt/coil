@file:Suppress("unused")

package coil.sample

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
import androidx.multidex.MultiDexApplication
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.fetch.VideoFrameFileFetcher
import coil.fetch.VideoFrameUriFetcher
import coil.util.DebugLogger
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

class Application : MultiDexApplication(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .availableMemoryPercentage(0.5) // Use 50% of the application's available memory.
            .crossfade(true) // Show a short crossfade when loading images from network or disk.
            .componentRegistry {
                // Fetchers
                add(VideoFrameFileFetcher(this@Application))
                add(VideoFrameUriFetcher(this@Application))

                // Decoders
                if (SDK_INT >= P) {
                    add(ImageDecoderDecoder())
                } else {
                    add(GifDecoder())
                }
                add(SvgDecoder(this@Application))
            }
            .apply {
                // Enable logging to the standard Android log if this is a debug build.
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .okHttpClient {
                // Create a disk cache with "unlimited" size. Don't do this in production.
                // To create the an optimized Coil disk cache, use CoilUtils.createDefaultCache(context).
                val cacheDirectory = File(filesDir, "image_cache").apply { mkdirs() }
                val cache = Cache(cacheDirectory, Long.MAX_VALUE)

                // Rewrite the Cache-Control header to cache all responses for a year.
                val cacheControlInterceptor = ResponseHeaderInterceptor("Cache-Control", "max-age=31536000,public")

                // Lazily create the OkHttpClient that is used for network operations.
                OkHttpClient.Builder()
                    .cache(cache)
                    .forceTls12() // The Unsplash API requires TLS 1.2, which isn't enabled by default before Lollipop.
                    .addNetworkInterceptor(cacheControlInterceptor)
                    .build()
            }
            .build()
    }
}
