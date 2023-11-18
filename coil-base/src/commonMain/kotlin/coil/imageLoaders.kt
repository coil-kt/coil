package coil

/**
 * Create a new [ImageLoader] without configuration.
 */
fun ImageLoader(context: PlatformContext): ImageLoader {
    return ImageLoader.Builder(context).build()
}

// addLastModifiedToFileCacheKey

/**
 * Enables adding a file's last modified timestamp to the memory cache key when loading an image
 * from a file.
 *
 * This allows subsequent requests that load the same file to miss the memory cache if the
 * file has been updated. However, if the memory cache check occurs on the main thread
 * (see [ImageLoader.Builder.interceptorDispatcher]) calling this will cause a strict mode
 * violation.
 */
fun ImageLoader.Builder.addLastModifiedToFileCacheKey(enable: Boolean) = apply {
    extras[addLastModifiedToFileCacheKeyKey] = enable
}

internal val RealImageLoader.Options.addLastModifiedToFileCacheKey: Boolean
    get() = extras.getOrDefault(addLastModifiedToFileCacheKeyKey)

private val addLastModifiedToFileCacheKeyKey = Extras.Key(default = true)

// networkObserverEnabled

/**
 * Enables short circuiting network requests if the device is offline.
 *
 * If true, reading from the network will automatically be disabled if the device is
 * offline. If a cached response is unavailable the request will fail with a
 * '504 Unsatisfiable Request' response.
 *
 * If false, the image loader will attempt a network request even if the device is offline.
 */
fun ImageLoader.Builder.networkObserverEnabled(enable: Boolean) = apply {
    extras[networkObserverEnabledKey] = enable
}

internal val RealImageLoader.Options.networkObserverEnabled: Boolean
    get() = extras.getOrDefault(networkObserverEnabledKey)

private val networkObserverEnabledKey = Extras.Key(default = true)
