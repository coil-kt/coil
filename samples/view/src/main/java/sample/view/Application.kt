package sample.view

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import sample.common.BuildConfig
import sample.common.newImageLoader

class Application : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return newImageLoader(this, BuildConfig.DEBUG)
    }
}
