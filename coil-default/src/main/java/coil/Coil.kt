@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package coil

import android.content.Context
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
    @Deprecated(
        message = "Migrate to loader(context).",
        replaceWith = ReplaceWith("loader(context)")
    )
    @JvmStatic
    fun loader(): ImageLoader = imageLoader ?: buildDefaultImageLoader(CoilContentProvider.context)

    /**
     * Get the default [ImageLoader] instance. Creates a new instance if none has been set.
     */
    @JvmStatic
    fun loader(context: Context): ImageLoader = imageLoader ?: buildDefaultImageLoader(context)

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
    private fun buildDefaultImageLoader(context: Context): ImageLoader {
        // Check again in case imageLoader was just set.
        imageLoader?.let { return it }

        // Create a new ImageLoader.
        val loader = imageLoaderInitializer?.invoke() ?: ImageLoader(context)
        imageLoaderInitializer = null
        setDefaultImageLoader(loader)
        return loader
    }
}
