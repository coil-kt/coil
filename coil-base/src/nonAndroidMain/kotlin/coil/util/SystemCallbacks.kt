package coil.util

import coil.RealImageLoader

internal actual fun SystemCallbacks(
    options: RealImageLoader.Options,
): SystemCallbacks = NoopSystemCallbacks()

// TODO: Listen for memory-pressure events to trim the memory cache on non-Android platforms.
private class NoopSystemCallbacks : SystemCallbacks {
    override val isOnline get() = true

    // TODO: Convert to atomic once https://github.com/Kotlin/kotlinx-atomicfu/issues/365 is fixed.
    private var _isShutdown = false
    override val isShutdown get() = _isShutdown

    override fun register(imageLoader: RealImageLoader) {}

    override fun shutdown() {
        _isShutdown = true
    }
}
