package coil

interface PlatformContext {

    /** Return the global application context. */
    val application: PlatformContext

    /** Return the total available memory for this application. */
    val totalAvailableMemoryBytes: Long
}
