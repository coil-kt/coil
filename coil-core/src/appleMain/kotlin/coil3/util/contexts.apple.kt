package coil3.util

import coil3.PlatformContext
import platform.Foundation.NSProcessInfo

internal actual fun PlatformContext.totalAvailableMemoryBytes(): Long {
    return NSProcessInfo.processInfo.physicalMemory.toLong()
}
