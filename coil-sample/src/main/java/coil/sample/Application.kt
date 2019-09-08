@file:Suppress("unused")

package coil.sample

import androidx.multidex.MultiDexApplication
import coil.Coil
import coil.ImageLoader
import coil.util.CoilLogger
import coil.util.CoilUtils
import okhttp3.OkHttpClient

class Application : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        CoilLogger.setEnabled(true) // Enable logging to the standard Android log.
        Coil.setDefaultImageLoader(::buildDefaultImageLoader) // Set a callback to lazily initialize the default ImageLoader.
    }

    private fun buildDefaultImageLoader(): ImageLoader {
        return ImageLoader(applicationContext) {
            availableMemoryPercentage(0.5) // Use 50% of the application's available memory.
            bitmapPoolPercentage(0.5) // Use 50% of the memory allocated to this ImageLoader for the bitmap pool.
            crossfade(true) // Show a short crossfade when loading images from network or disk into an ImageView.
            okHttpClient {
                // Lazily create the OkHttpClient that is used for network operations.
                OkHttpClient.Builder()
                    .cache(CoilUtils.createDefaultCache(applicationContext))
                    .forceTls12() // The Unsplash API requires TLS 1.2, which isn't enabled by default before Lollipop.
                    .build()
            }
        }
    }
}
