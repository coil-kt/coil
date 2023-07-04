package coil.util

import coil.RealImageLoader
import kotlinx.atomicfu.atomic

internal actual fun SystemCallbacks(
    options: RealImageLoader.Options,
): SystemCallbacks = NoopSystemCallbacks()

// TODO: Listen for memory-pressure events to trim the memory cache on non-Android platforms.
private class NoopSystemCallbacks : SystemCallbacks {
    override val isOnline get() = true

    override var isShutdown by atomic(false)

    override fun register(imageLoader: RealImageLoader) {}

    override fun shutdown() {
        isShutdown = true
    }
}
