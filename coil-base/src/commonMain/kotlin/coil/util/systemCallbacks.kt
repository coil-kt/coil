package coil.util

import coil.RealImageLoader

internal expect fun SystemCallbacks(
    options: RealImageLoader.Options,
): SystemCallbacks

internal interface SystemCallbacks {
    fun register(imageLoader: RealImageLoader)
    fun shutdown()
}
