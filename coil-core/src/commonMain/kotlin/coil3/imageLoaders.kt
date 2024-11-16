package coil3

import coil3.annotation.ExperimentalCoilApi
import coil3.map.Mapper
import coil3.key.Keyer
import coil3.memory.MemoryCache
import coil3.intercept.Interceptor

/**
 * Create a new [ImageLoader] without configuration.
 */
fun ImageLoader(context: PlatformContext): ImageLoader {
    return ImageLoader.Builder(context).build()
}

// region serviceLoaderComponentsEnabled

/**
 * Enables adding all components (fetchers and decoders) that are supported by the service locator
 * to this [ImageLoader]'s [ComponentRegistry]. All of Coil's first party decoders and fetchers are
 * supported.
 *
 * If true, all components that are supported by the service locator will be added to this
 * [ImageLoader]'s [ComponentRegistry].
 *
 * If false, no components from the service locator will be added to the [ImageLoader]'s
 * [ComponentRegistry].
 */
fun ImageLoader.Builder.serviceLoaderEnabled(enable: Boolean) = apply {
    extras[serviceLoaderEnabledKey] = enable
}

internal val RealImageLoader.Options.serviceLoaderEnabled: Boolean
    get() = defaults.extras.getOrDefault(serviceLoaderEnabledKey)

private val serviceLoaderEnabledKey = Extras.Key(default = true)

// endregion
// region checkMemoryCacheBeforeInterceptorChain

/**
 * If true, the [ImageLoader]'s [Mapper]s, [Keyer]s, and [MemoryCache] check will be run BEFORE its
 * [Interceptor]s are invoked.
 *
 * If false, the [ImageLoader]'s [Mapper]s, [Keyer]s, and [MemoryCache] check will be run AFTER its
 * [Interceptor]s are invoked.
 */
@ExperimentalCoilApi
fun ImageLoader.Builder.checkMemoryCacheBeforeInterceptorChain(enable: Boolean) = apply {
    extras[checkMemoryCacheBeforeInterceptorChainKey] = enable
}

internal val RealImageLoader.Options.checkMemoryCacheBeforeInterceptorChain: Boolean
    get() = defaults.extras.getOrDefault(checkMemoryCacheBeforeInterceptorChainKey)

private val checkMemoryCacheBeforeInterceptorChainKey = Extras.Key(default = false)

// endregion
