package coil3.test.utils

import coil3.PlatformContext

actual abstract class AndroidJUnit4Test

actual abstract class RobolectricTest

actual val context: PlatformContext
    get() = PlatformContext.INSTANCE
