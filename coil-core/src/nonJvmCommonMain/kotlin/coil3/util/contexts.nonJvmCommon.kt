package coil3.util

import coil3.PlatformContext

internal actual fun PlatformContext.totalAvailableMemoryBytes(): Long {
    // TODO: Figure out how to compute the total available memory on non-JVM platforms.
    return 512L * 1024L * 1024L // 512 MB
}
