package coil

internal actual fun PlatformContext.applicationImageLoaderFactory(): ImageLoaderFactory? {
    return application.asAndroidContext() as? ImageLoaderFactory
}
