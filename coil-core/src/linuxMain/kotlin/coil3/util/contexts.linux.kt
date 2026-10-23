package coil3.util

import coil3.PlatformContext

internal actual fun PlatformContext.totalAvailableMemoryBytes(): Long {
    return 512L * 1024 * 1024 // 512 MB
}
