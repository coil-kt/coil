package coil3

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
