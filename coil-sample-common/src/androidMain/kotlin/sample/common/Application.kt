package sample.common

import android.app.Application
import coil.ImageLoader
import coil.SingletonImageLoader

class Application : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(): ImageLoader {
        return newImageLoader(this, BuildConfig.DEBUG)
    }
}
