package coil3.util

import coil3.PlatformContext
import platform.Foundation.NSProcessInfo

internal actual fun PlatformContext.totalAvailableMemoryBytes(): Long {
    // A single app process is budgeted a fraction of the total physical memory.
    // We estimate the process budget to be 25% of physical memory, capped at 1 GB.
    val physicalMemory = NSProcessInfo.processInfo.physicalMemory.toLong()
    return (physicalMemory / 4).coerceAtMost(1024L * 1024 * 1024)
}
