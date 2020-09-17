@file:Suppress("NOTHING_TO_INLINE", "unused")

package coil

import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting
import coil.request.Disposable
import coil.request.ImageRequest
import coil.request.ImageResult

/**
 * A class that holds the singleton [ImageLoader] instance.
 */
object Coil {

    private var imageLoader: ImageLoader? = null
    private var imageLoaderFactory: ImageLoaderFactory? = null

    /**
     * Get the singleton [ImageLoader]. Creates a new instance if none has been set.
     */
    @JvmStatic
    fun imageLoader(context: Context): ImageLoader = imageLoader ?: newImageLoader(context)

    /**
     * Convenience function to get the singleton [ImageLoader] and enqueue the [request].
     *
     * @see ImageLoader.enqueue
     */
    @JvmStatic
    inline fun enqueue(request: ImageRequest): Disposable {
        return imageLoader(request.context).enqueue(request)
    }

    /**
     * Convenience function to get the singleton [ImageLoader] and execute the [request].
     *
     * @see ImageLoader.execute
     */
    @JvmStatic
    suspend inline fun execute(request: ImageRequest): ImageResult {
        return imageLoader(request.context).execute(request)
    }

    /**
     * Set the singleton [ImageLoader]. Prefer using `setImageLoader(ImageLoaderFactory)`
     * to create the [ImageLoader] lazily.
     */
    @JvmStatic
    @Synchronized
    fun setImageLoader(imageLoader: ImageLoader) {
        this.imageLoaderFactory = null
        this.imageLoader = imageLoader
    }

    /**
     * Set the [ImageLoaderFactory] that will be used to create the singleton [ImageLoader].
     * The [factory] is guaranteed to be called at most once.
     *
     * NOTE: [factory] will take precedence over an [Application] that implements [ImageLoaderFactory].
     */
    @JvmStatic
    @Synchronized
    fun setImageLoader(factory: ImageLoaderFactory) {
        imageLoaderFactory = factory
        imageLoader = null
    }

    /** Create and set the new singleton [ImageLoader]. */
    @Synchronized
    private fun newImageLoader(context: Context): ImageLoader {
        // Check again in case imageLoader was just set.
        imageLoader?.let { return it }

        // Create a new ImageLoader.
        val newImageLoader = imageLoaderFactory?.newImageLoader()
            ?: (context.applicationContext as? ImageLoaderFactory)?.newImageLoader()
            ?: ImageLoader(context)
        imageLoaderFactory = null
        imageLoader = newImageLoader
        return newImageLoader
    }

    /** Reset the internal state. */
    @VisibleForTesting
    @Synchronized
    internal fun reset() {
        imageLoader = null
        imageLoaderFactory = null
    }
}
