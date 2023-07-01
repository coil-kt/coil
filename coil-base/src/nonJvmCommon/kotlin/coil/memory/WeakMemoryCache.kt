package coil.memory

// Weak references are currently only available on the JVM.
internal actual fun RealWeakMemoryCache(): WeakMemoryCache = EmptyWeakMemoryCache()
