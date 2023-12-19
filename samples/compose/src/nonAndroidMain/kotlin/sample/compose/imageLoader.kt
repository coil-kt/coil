package sample.compose

import coil3.PlatformContext
import coil3.SingletonImageLoader
import sample.common.newImageLoader

fun initializeSingletonImageLoader() {
    // Set the singleton image loader exactly once.
    initialize.value
}

private val initialize = lazy {
    SingletonImageLoader.set {
        newImageLoader(PlatformContext.INSTANCE)
    }
}
