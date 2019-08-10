@file:JvmName("Coil")
@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package coil

import coil.util.CoilContentProvider

/**
 * A singleton that holds the default [ImageLoader] instance.
 */
object Coil {

    private var imageLoader: ImageLoader? = null
    private var imageLoaderFactory: (() -> ImageLoader)? = null

    /**
     * Get the default [ImageLoader] instance. Creates a new instance if none has been set.
     */
    @JvmStatic
    fun loader(): ImageLoader = imageLoader ?: buildDefaultImageLoader()

    /**
     * Set the default [ImageLoader] instance. Shutdown the current instance.
     */
    @JvmStatic
    fun setDefaultImageLoader(loader: ImageLoader) {
        imageLoader?.shutdown()
        imageLoader = loader
        imageLoaderFactory = null
    }

    /**
     * Set the factory for the default [ImageLoader] instance. Shutdown the current instance.
     *
     * The [factory] is guaranteed to only be called once. This enables lazy instantiation of the default [ImageLoader].
     */
    @JvmStatic
    fun setDefaultImageLoader(factory: () -> ImageLoader) {
        imageLoader?.shutdown()
        imageLoaderFactory = factory
        imageLoader = null
    }

    @Synchronized
    private fun buildDefaultImageLoader(): ImageLoader {
        // Check again in case imageLoader was just set.
        return imageLoader ?: run {
            val loader = imageLoaderFactory?.invoke() ?: ImageLoader(CoilContentProvider.context)
            imageLoaderFactory = null
            setDefaultImageLoader(loader)
            loader
        }
    }
}
