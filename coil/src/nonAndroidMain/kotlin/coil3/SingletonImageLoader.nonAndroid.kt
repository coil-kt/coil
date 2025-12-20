package coil3

internal actual fun PlatformContext.applicationImageLoaderFactory(): SingletonImageLoader.Factory? {
    return null
}

/**
 * @return the PlatformContext as is, for non-Android platform.
 */
internal actual fun PlatformContext.applicationContext(): PlatformContext = this
