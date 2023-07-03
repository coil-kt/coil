package coil

private object DefaultPlatformContext : PlatformContext {

    override val application: PlatformContext
        get() = this

    override val totalAvailableMemory: Long
        get() = 512L * 1024L * 1024L // 512 MB
}
