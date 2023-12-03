package coil3.util

import coil3.PlatformContext

actual abstract class WithPlatformContext {
    actual val context get() = PlatformContext.INSTANCE
}
