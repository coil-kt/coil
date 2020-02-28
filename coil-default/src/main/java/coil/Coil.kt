@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package coil

import android.app.Application
import android.content.Context
import coil.util.CoilContentProvider

/**
 * A singleton that holds the default [ImageLoader] instance.
 */
object Coil {

    private var imageLoader: ImageLoader? = null
    private var imageLoaderProvider: ImageLoaderProvider? = null

    /** @see imageLoader */
    @Deprecated(
        message = "Migrate to imageLoader(context).",
        replaceWith = ReplaceWith("imageLoader(context)")
    )
    @JvmStatic
    fun loader(): ImageLoader = imageLoader(CoilContentProvider.context)

    /**
     * Get the default [ImageLoader]. Creates a new instance if none has been set.
     */
    @JvmStatic
    fun imageLoader(context: Context): ImageLoader = imageLoader ?: buildImageLoader(context)

    /**
     * Set the default [ImageLoader]. Shutdown the current instance if there is one.
     */
    @JvmStatic
    fun setImageLoader(loader: ImageLoader) {
        setImageLoader(object : ImageLoaderProvider {
            override fun getImageLoader() = loader
        })
    }

    /**
     * Set a lazy callback to create the default [ImageLoader]. Shutdown the current instance if there is one.
     * The [provider] is guaranteed to be called at most once.
     *
     * Using this method to set an explicit [provider] takes precedence over an [Application] that
     * implements [ImageLoaderProvider].
     */
    @JvmStatic
    @Synchronized
    fun setImageLoader(provider: ImageLoaderProvider) {
        imageLoaderProvider = provider

        // Shutdown the image loader after clearing the reference.
        val loader = imageLoader
        imageLoader = null
        loader?.shutdown()
    }

    /** @see setImageLoader */
    @Deprecated(
        message = "Migrate to setImageLoader(loader).",
        replaceWith = ReplaceWith("setImageLoader(loader)")
    )
    @JvmStatic
    fun setDefaultImageLoader(loader: ImageLoader) = setImageLoader(loader)

    /** @see setImageLoader */
    @Deprecated(
        message = "Migrate to setDefaultImageLoader(ImageLoaderProvider).",
        replaceWith = ReplaceWith("setImageLoader(object : ImageLoaderProvider { override fun getImageLoader() = provider() })")
    )
    @JvmStatic
    fun setDefaultImageLoader(provider: () -> ImageLoader) {
        setImageLoader(object : ImageLoaderProvider {
            override fun getImageLoader() = provider()
        })
    }

    @Synchronized
    private fun buildImageLoader(context: Context): ImageLoader {
        // Check again in case imageLoader was just set.
        imageLoader?.let { return it }

        // Create a new ImageLoader.
        val loader = imageLoaderProvider?.getImageLoader()
            ?: (context.applicationContext as? ImageLoaderProvider)?.getImageLoader()
            ?: ImageLoader(context)
        imageLoaderProvider = null
        setImageLoader(loader)
        return loader
    }
}
