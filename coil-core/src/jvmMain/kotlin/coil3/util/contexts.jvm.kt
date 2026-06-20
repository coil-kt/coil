package coil3.util

import coil3.PlatformContext

internal actual fun PlatformContext.totalAvailableMemoryBytes(): Long {
    val maxMemory = Runtime.getRuntime().maxMemory()
    return if (maxMemory == Long.MAX_VALUE) {
        8L * 1024 * 1024 * 1024 // 8 GB
    } else {
        maxMemory
    }
}
