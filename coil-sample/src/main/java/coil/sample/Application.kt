@file:Suppress("unused")

package coil.sample

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import androidx.multidex.MultiDexApplication
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderBuilder.Companion.applyCoilOptimizations
import coil.util.CoilLogger
import okhttp3.Call
import okhttp3.OkHttpClient

class Application : MultiDexApplication() {

    /**
     * Clever technique with Call.Factory. By lazily constructing our OkHttpClient instance,
     * we can defer its instantiation until the first network request and off the main thread.
     */
    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .applyCoilOptimizations(this@Application)
            .apply {
                // The Unsplash API requires TLS 1.2, which isn't enabled by default before Lollipop.
                if (SDK_INT < LOLLIPOP) forceTls12()
            }
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        CoilLogger.setEnabled(true)
        Coil.setDefaultImageLoader(::buildDefaultImageLoader)
    }

    private fun buildDefaultImageLoader(): ImageLoader {
        return ImageLoader(this) {
            availableMemoryPercentage(0.5)
            bitmapPoolPercentage(0.5)
            crossfade(true)
            callFactory(Call.Factory { request -> okHttpClient.newCall(request) })
        }
    }
}
