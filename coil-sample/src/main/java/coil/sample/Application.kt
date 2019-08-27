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
        CoilLogger.setEnabled(true)
        Coil.setDefaultImageLoader(::buildDefaultImageLoader)
    }

    private fun buildDefaultImageLoader(): ImageLoader {
        val context = this
        return ImageLoader(context) {
            availableMemoryPercentage(0.5)
            bitmapPoolPercentage(0.5)
            crossfade(true)
            okHttpClient {
                OkHttpClient.Builder()
                    .cache(CoilUtils.createDefaultCache(context))
                    .forceTls12() // The Unsplash API requires TLS 1.2, which isn't enabled by default before Lollipop.
                    .build()
            }
        }
    }
}
