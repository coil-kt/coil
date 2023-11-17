package coil

import kotlin.jvm.JvmField

actual class PlatformContext private constructor() {
    companion object {
        @JvmField val INSTANCE = PlatformContext()
    }
}
