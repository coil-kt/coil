package coil3.test

import coil3.PlatformContext

actual abstract class WithPlatformContext {
    actual val context get() = PlatformContext.INSTANCE
}
