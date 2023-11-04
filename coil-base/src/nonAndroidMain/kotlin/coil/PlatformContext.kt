package coil

private object DefaultPlatformContext : PlatformContext {

    override val application: PlatformContext
        get() = this

    // TODO: Compute the total available memory on non-Android platforms.
    override val totalAvailableMemoryBytes: Long
        get() = 512L * 1024L * 1024L // 512 MB
}
