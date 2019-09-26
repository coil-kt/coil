@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package coil

import coil.util.CoilContentProvider

/**
 * A singleton that holds the default [ImageLoader] instance.
 */
object Coil {

    private var imageLoader: ImageLoader? = null
    private var imageLoaderInitializer: (() -> ImageLoader)? = null

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
        imageLoaderInitializer = null
    }

    /**
     * Set a lazy callback to create the default [ImageLoader] instance. Shutdown the current instance.
     *
     * The [initializer] is guaranteed to only be called once. This enables lazy instantiation of the default [ImageLoader].
     */
    @JvmStatic
    fun setDefaultImageLoader(initializer: () -> ImageLoader) {
        imageLoader?.shutdown()
        imageLoaderInitializer = initializer
        imageLoader = null
    }

    @Synchronized
    private fun buildDefaultImageLoader(): ImageLoader {
        // Check again in case imageLoader was just set.
        return imageLoader ?: run {
            val loader = imageLoaderInitializer?.invoke() ?: ImageLoader(CoilContentProvider.context)
            imageLoaderInitializer = null
            setDefaultImageLoader(loader)
            loader
        }
    }
}
