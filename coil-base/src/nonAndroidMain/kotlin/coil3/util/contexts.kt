package coil3.util

import coil3.PlatformContext

internal actual val PlatformContext.application: PlatformContext
    get() = this

internal actual fun PlatformContext.defaultMemoryCacheSizePercent(): Double {
    return 0.15
}

// TODO: Compute the total available memory on non-Android platforms.
internal actual fun PlatformContext.totalAvailableMemoryBytes(): Long {
    return 512L * 1024L * 1024L // 512 MB
}
