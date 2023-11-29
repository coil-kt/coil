package coil.util

import coil.PlatformContext

actual fun PlatformContext(): PlatformContext {
    return PlatformContext.INSTANCE
}

actual abstract class PlatformContextTest
