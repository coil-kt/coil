package coil

internal actual fun PlatformContext.applicationImageLoaderFactory(): SingletonImageLoader.Factory? {
    return application.asAndroidContext() as? SingletonImageLoader.Factory
}
