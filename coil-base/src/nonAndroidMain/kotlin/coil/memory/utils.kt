package coil.memory

import coil.PlatformContext

internal actual fun PlatformContext.defaultMemoryCacheSizePercent(): Double {
    return 0.15
}
