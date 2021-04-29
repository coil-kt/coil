@file:Suppress("NOTHING_TO_INLINE", "unused")

package coil

import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting
import coil.Coil.setImageLoader

/**
 * A class that holds the singleton [ImageLoader] instance.
 *
 * - To get the singleton [ImageLoader] use [Context.imageLoader].
 * - To set the singleton [ImageLoader] use [setImageLoader] or see [ImageLoaderFactory].
 */
object Coil {

    private var imageLoader: ImageLoader? = null
    private var imageLoaderFactory: ImageLoaderFactory? = null

    /**
     * Set the singleton [ImageLoader].
     * Prefer using `setImageLoader(ImageLoaderFactory)` to create the [ImageLoader] lazily.
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

    /** Get the singleton [ImageLoader]. Use [Context.imageLoader] for a public API. */
    internal fun imageLoader(context: Context) = imageLoader ?: newImageLoader(context)

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
