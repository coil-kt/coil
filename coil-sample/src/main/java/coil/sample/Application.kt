@file:Suppress("unused")

package coil.sample

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
import androidx.multidex.MultiDexApplication
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import coil.util.CoilLogger
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

class Application : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        CoilLogger.setEnabled(BuildConfig.DEBUG) // Enable logging to the standard Android log if this is a debug build.
        Coil.setDefaultImageLoader(::buildDefaultImageLoader) // Set a callback to lazily initialize the default ImageLoader.
    }

    private fun buildDefaultImageLoader(): ImageLoader {
        return ImageLoader(applicationContext) {
            availableMemoryPercentage(0.5) // Use 50% of the application's available memory.
            crossfade(true) // Show a short crossfade when loading images from network or disk into an ImageView.
            componentRegistry {
                if (SDK_INT >= P) {
                    add(ImageDecoderDecoder())
                } else {
                    add(GifDecoder())
                }
                add(SvgDecoder(applicationContext))
                add(VideoFrameDecoder(applicationContext))
            }
            okHttpClient {
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
        }
    }
}
