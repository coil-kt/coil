@file:Suppress("unused")

package coil.sample

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import androidx.multidex.MultiDexApplication
import coil.Coil
import coil.ImageLoader
import coil.util.CoilLogger
import coil.util.applyCoilOptimizations
import okhttp3.OkHttpClient

class Application : MultiDexApplication() {

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
            okHttpClient(
                OkHttpClient.Builder()
                    .applyCoilOptimizations(this@Application)
                    .apply {
                        // The Unsplash API requires TLS 1.2, which isn't enabled by default before Lollipop.
                        if (SDK_INT < LOLLIPOP) forceTls12()
                    }
                    .build()
            )
        }
    }
}
