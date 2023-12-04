package coil3.test

import coil3.PlatformContext

actual abstract class RobolectricTest

actual val context: PlatformContext
    get() = PlatformContext.INSTANCE
