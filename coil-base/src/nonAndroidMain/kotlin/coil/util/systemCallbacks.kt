package coil.util

import coil.RealImageLoader

internal actual fun SystemCallbacks(
    options: RealImageLoader.Options,
): SystemCallbacks = NoopSystemCallbacks()

// TODO: Listen for memory-pressure events to trim the memory cache on non-Android platforms.
private class NoopSystemCallbacks : SystemCallbacks {
    override fun register(imageLoader: RealImageLoader) {}
    override fun shutdown() {}
}
