package coil3.util

import coil3.RealImageLoader

internal expect fun SystemCallbacks(
    imageLoader: RealImageLoader,
): SystemCallbacks

internal interface SystemCallbacks {
    val isOnline: Boolean

    fun registerMemoryPressureCallbacks()
    fun shutdown()
}
