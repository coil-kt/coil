package coil3.test.utils

import coil3.PlatformContext

actual abstract class AndroidJUnit4Test

actual abstract class RobolectricTest

actual inline val context: PlatformContext
    get() = PlatformContext
