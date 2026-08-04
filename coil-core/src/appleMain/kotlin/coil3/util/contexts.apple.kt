package coil3.util

import coil3.PlatformContext
import platform.Foundation.NSProcessInfo

internal actual fun PlatformContext.totalAvailableMemoryBytes(): Long {
    // NSProcessInfo.physicalMemory returns the total physical RAM as a ULong.
    // Clamp to Long.MAX_VALUE for safety on theoretical future hardware.
    val physicalMemory = NSProcessInfo.processInfo.physicalMemory
    return if (physicalMemory > Long.MAX_VALUE.toULong()) Long.MAX_VALUE else physicalMemory.toLong()
}

