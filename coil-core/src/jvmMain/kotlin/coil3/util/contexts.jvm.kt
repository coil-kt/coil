package coil3.util

import coil3.PlatformContext

internal actual fun PlatformContext.totalAvailableMemoryBytes(): Long {
    return Runtime.getRuntime().maxMemory()
}
