package coil

import coil.util.internalExtraKeyOf

/**
 * Create a new [ImageLoader] without configuration.
 */
fun ImageLoader(context: Context): ImageLoader {
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
 *
 * Default: [addLastModifiedToFileCacheKeyDefault]
 */
fun ImageLoader.Builder.addLastModifiedToFileCacheKey(enable: Boolean) = apply {
    extra(addLastModifiedToFileCacheKeyKey, enable)
}

internal val RealImageLoader.Options.addLastModifiedToFileCacheKey: Boolean
    get() = extras.get(addLastModifiedToFileCacheKeyKey) ?: addLastModifiedToFileCacheKeyDefault

private val addLastModifiedToFileCacheKeyKey = internalExtraKeyOf("addLastModifiedToFileCacheKey")
private const val addLastModifiedToFileCacheKeyDefault = true

// networkObserverEnabled

/**
 * Enables short circuiting network requests if the device is offline.
 *
 * If true, reading from the network will automatically be disabled if the device is
 * offline. If a cached response is unavailable the request will fail with a
 * '504 Unsatisfiable Request' response.
 *
 * If false, the image loader will attempt a network request even if the device is offline.
 *
 * Default: [networkObserverEnabledDefault]
 */
fun ImageLoader.Builder.networkObserverEnabled(enable: Boolean) = apply {
    extra(networkObserverEnabledKey, enable)
}

internal val RealImageLoader.Options.networkObserverEnabled: Boolean
    get() = extras.get(networkObserverEnabledKey) ?: networkObserverEnabledDefault

private val networkObserverEnabledKey = internalExtraKeyOf("networkObserverEnabled")
private const val networkObserverEnabledDefault = true
