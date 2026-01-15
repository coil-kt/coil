package coil3

internal actual fun PlatformContext.applicationImageLoaderFactory(): SingletonImageLoader.Factory? {
    return applicationContext as? SingletonImageLoader.Factory
}

/**
 * Converts PlatformContext to ApplicationContext if possible.
 * Otherwise returns the platform context as is.
 * @return PlatformContext's applicationContext for Android.
 */
internal actual fun PlatformContext.applicationContext(): PlatformContext =
    try {
        this.applicationContext
    } catch (exception: Exception) {
        // Return the platform context as is just in case.
        this
    }
