package coil

import coil.util.internalExtraKeyOf

/**
 * Create a new [ImageLoader] without configuration.
 */
fun ImageLoader(context: PlatformContext): ImageLoader {
    return ImageLoader.Builder(context).build()
}

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

private val addLastModifiedToFileCacheKeyKey = internalExtraKeyOf("addLastModifiedToFileCacheKey")
private const val addLastModifiedToFileCacheKeyDefault = true
internal val RealImageLoader.Options.addLastModifiedToFileCacheKey
    get() = extras.get(addLastModifiedToFileCacheKeyKey) ?: addLastModifiedToFileCacheKeyDefault

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

private val networkObserverEnabledKey = internalExtraKeyOf("networkObserverEnabled")
private const val networkObserverEnabledDefault = true
internal val RealImageLoader.Options.networkObserverEnabled
    get() = extras.get(networkObserverEnabledKey) ?: networkObserverEnabledDefault

/**
 * Enables support for network cache headers. If enabled, this image loader will respect the
 * cache headers returned by network responses when deciding if an image can be stored or
 * served from the disk cache. If disabled, images will always be served from the disk cache
 * (if present) and will only be evicted to stay under the maximum size.
 *
 * Default: [respectCacheHeadersDefault]
 */
fun ImageLoader.Builder.respectCacheHeaders(enable: Boolean) = apply {
    extra(respectCacheHeadersKey, enable)
}

private val respectCacheHeadersKey = internalExtraKeyOf("respectCacheHeaders")
private const val respectCacheHeadersDefault = true
internal val RealImageLoader.Options.respectCacheHeaders
    get() = extras.get(respectCacheHeadersKey) ?: respectCacheHeadersDefault
