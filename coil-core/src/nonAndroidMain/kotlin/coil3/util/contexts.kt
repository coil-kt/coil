package coil3.util

import coil3.PlatformContext

internal actual val PlatformContext.application: PlatformContext
    get() = this

internal actual fun PlatformContext.defaultMemoryCacheSizePercent(): Double {
    return 0.15
}
