package coil3.util

import coil3.PlatformContext

internal actual fun PlatformContext.totalAvailableMemoryBytes(): Long {
    // No standard API is available to query memory in browser / Node.js environments.
    return 512L * 1024L * 1024L // 512 MB
}

