package coil3

import coil3.key.Keyer

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

/**
 * Enables the default [Keyer]. This means that data types that don't have a registered [Keyer]
 * will have a memory cache key created for them using [Any.toString].
 *
 * If disabled (the default), images will not be cached if they do not have a registered [Keyer]
 * for the associated data type.
 */
fun ImageLoader.Builder.defaultKeyerEnabled(enable: Boolean) = apply {
    extras[defaultKeyerEnabledKey] = enable
}

internal val RealImageLoader.Options.defaultKeyerEnabled: Boolean
    get() = defaults.extras.getOrDefault(defaultKeyerEnabledKey)

private val defaultKeyerEnabledKey = Extras.Key(default = false)
