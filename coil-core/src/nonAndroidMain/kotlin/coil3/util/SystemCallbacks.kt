package coil3.util

import coil3.RealImageLoader

internal actual fun SystemCallbacks(
    imageLoader: RealImageLoader,
): SystemCallbacks = NoopSystemCallbacks()

private class NoopSystemCallbacks : SystemCallbacks {
    override val isOnline get() = true

    // TODO: Listen for memory-pressure events to trim the memory cache on non-Android platforms.
    override fun registerMemoryPressureCallbacks() {}
    override fun shutdown() {}
}
