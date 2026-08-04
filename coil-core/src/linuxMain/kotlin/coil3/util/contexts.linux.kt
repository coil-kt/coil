package coil3.util

import coil3.PlatformContext

internal actual fun PlatformContext.totalAvailableMemoryBytes(): Long {
    // TODO: Compute real available memory on Linux using sysinfo() or /proc/meminfo.
    return 512L * 1024L * 1024L // 512 MB
}

