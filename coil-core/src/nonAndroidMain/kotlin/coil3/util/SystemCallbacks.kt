package coil3.util

import coil3.RealImageLoader
import kotlinx.atomicfu.atomic

internal actual fun SystemCallbacks(): SystemCallbacks = NoopSystemCallbacks()

// TODO: Listen for memory-pressure events to trim the memory cache on non-Android platforms.
private class NoopSystemCallbacks : SystemCallbacks {
    override val isOnline get() = true
    override var isShutdown by atomic(false)

    override fun register(imageLoader: RealImageLoader) {}

    override fun shutdown() {
        isShutdown = true
    }
}
