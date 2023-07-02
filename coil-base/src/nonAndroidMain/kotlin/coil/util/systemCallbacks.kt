package coil.util

import coil.RealImageLoader

internal actual fun SystemCallbacks(
    options: RealImageLoader.Options,
): SystemCallbacks = object : SystemCallbacks {
    override fun register(imageLoader: RealImageLoader) {}
    override fun shutdown() {}
}
