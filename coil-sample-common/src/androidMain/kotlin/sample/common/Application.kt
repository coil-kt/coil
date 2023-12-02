package sample.common

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader

class Application : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(): ImageLoader {
        return newImageLoader(this, BuildConfig.DEBUG)
    }
}
