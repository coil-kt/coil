package coil

internal actual fun Context.applicationImageLoaderFactory(): SingletonImageLoader.Factory? {
    return applicationContext as? SingletonImageLoader.Factory
}
