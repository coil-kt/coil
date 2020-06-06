@file:Suppress("NOTHING_TO_INLINE", "unused")

package coil

import android.app.Application
import android.content.Context
import coil.request.ImageRequest
import coil.request.RequestDisposable
import coil.request.RequestResult

/**
 * A singleton that holds the default [ImageLoader] instance.
 */
object Coil {

    private var imageLoader: ImageLoader? = null
    private var imageLoaderFactory: ImageLoaderFactory? = null

    /**
     * Get the default [ImageLoader]. Creates a new instance if none has been set.
     */
    @JvmStatic
    fun imageLoader(context: Context): ImageLoader = imageLoader ?: newImageLoader(context)

    /**
     * Set the default [ImageLoader]. Shutdown the current instance if there is one.
     */
    @JvmStatic
    fun setImageLoader(loader: ImageLoader) {
        setImageLoader(object : ImageLoaderFactory {
            override fun newImageLoader() = loader
        })
    }

    /**
     * Convenience function to get the default [ImageLoader] and enqueue the [request].
     *
     * @see ImageLoader.enqueue
     */
    @JvmStatic
    inline fun enqueue(request: ImageRequest): RequestDisposable {
        return imageLoader(request.context).enqueue(request)
    }

    /**
     * Convenience function to get the default [ImageLoader] and execute the [request].
     *
     * @see ImageLoader.execute
     */
    @JvmStatic
    suspend inline fun execute(request: ImageRequest): RequestResult {
        return imageLoader(request.context).execute(request)
    }

    /**
     * Set the [ImageLoaderFactory] that will be used to create the default [ImageLoader].
     * Shutdown the current instance if there is one. The [factory] is guaranteed to be called at most once.
     *
     * Using this method to set an explicit [factory] takes precedence over an [Application] that
     * implements [ImageLoaderFactory].
     */
    @JvmStatic
    @Synchronized
    fun setImageLoader(factory: ImageLoaderFactory) {
        imageLoaderFactory = factory

        // Shutdown the image loader after clearing the reference.
        val loader = imageLoader
        imageLoader = null
        loader?.shutdown()
    }

    /** Create and set the new default [ImageLoader]. */
    @Synchronized
    private fun newImageLoader(context: Context): ImageLoader {
        // Check again in case imageLoader was just set.
        imageLoader?.let { return it }

        // Create a new ImageLoader.
        val loader = imageLoaderFactory?.newImageLoader()
            ?: (context.applicationContext as? ImageLoaderFactory)?.newImageLoader()
            ?: ImageLoader(context)
        imageLoaderFactory = null
        setImageLoader(loader)
        return loader
    }
}
