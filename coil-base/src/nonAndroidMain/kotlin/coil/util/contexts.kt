package coil.util

import coil.Context

internal actual val Context.application: Context
    get() = this

internal actual fun Context.defaultMemoryCacheSizePercent(): Double {
    return 0.15
}

// TODO: Compute the total available memory on non-Android platforms.
internal actual fun Context.totalAvailableMemoryBytes(): Long {
    return 512L * 1024L * 1024L // 512 MB
}
