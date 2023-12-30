package coil3.util

import coil3.RealImageLoader

internal expect fun SystemCallbacks(): SystemCallbacks

internal interface SystemCallbacks {
    val isOnline: Boolean
    val isShutdown: Boolean

    fun register(imageLoader: RealImageLoader)
    fun shutdown()
}
