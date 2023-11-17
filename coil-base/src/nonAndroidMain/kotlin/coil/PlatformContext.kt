package coil

actual class PlatformContext private constructor() {
    companion object {
        val INSTANCE = PlatformContext()
    }
}
