package coil3

internal actual fun PlatformContext.applicationImageLoaderFactory(): SingletonImageLoader.Factory? {
    return applicationContext as? SingletonImageLoader.Factory
}
