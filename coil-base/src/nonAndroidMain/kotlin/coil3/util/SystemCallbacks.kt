package coil3.util

import coil3.RealImageLoader
import kotlinx.atomicfu.atomic

internal actual fun SystemCallbacks(): SystemCallbacks = NoopSystemCallbacks()

// TODO: Listen for memory-pressure events to trim the memory cache on non-Android platforms.
private class NoopSystemCallbacks : SystemCallbacks {
    override val isOnline get() = true

    private val _isShutdown = atomic(false)
    override val isShutdown: Boolean by _isShutdown

    override fun register(imageLoader: RealImageLoader) {}

    override fun shutdown() {
        _isShutdown.value = true
    }
}
