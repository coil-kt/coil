package coil.util

import coil.RealImageLoader

internal expect fun SystemCallbacks(
    options: RealImageLoader.Options,
): SystemCallbacks

internal interface SystemCallbacks {
    val isOnline: Boolean
    val isShutdown: Boolean

    fun register(imageLoader: RealImageLoader)
    fun shutdown()
}
