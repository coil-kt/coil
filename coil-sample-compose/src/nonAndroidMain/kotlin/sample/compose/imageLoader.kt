package sample.compose

import coil3.PlatformContext
import coil3.SingletonImageLoader
import kotlinx.atomicfu.atomic
import sample.common.newImageLoader

fun initializeSingletonImageLoader() {
    if (isInitialized.getAndSet(true)) return

    SingletonImageLoader.set {
        newImageLoader(PlatformContext.INSTANCE)
    }
}

private val isInitialized = atomic(false)
