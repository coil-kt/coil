package coil3.util

import coil3.PlatformContext

actual fun PlatformContext(): PlatformContext {
    return PlatformContext.INSTANCE
}

actual abstract class PlatformContextTest
