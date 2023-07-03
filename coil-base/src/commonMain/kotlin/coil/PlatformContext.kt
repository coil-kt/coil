package coil

interface PlatformContext {

    val application: PlatformContext

    val totalAvailableMemory: Long
}
