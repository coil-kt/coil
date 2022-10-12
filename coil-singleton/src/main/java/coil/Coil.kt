package coil

import android.app.Application
import android.content.Context
import coil.Coil.imageLoader
import coil.Coil.setImageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import coil.request.ImageResult

/**
 * A class that holds the singleton [ImageLoader] instance.
 *
 * - To get the singleton [ImageLoader] use [Context.imageLoader] (preferred) or [imageLoader].
 * - To set the singleton [ImageLoader] use [ImageLoaderFactory] (preferred) or [setImageLoader].
 */
object Coil {

    private var imageLoader: ImageLoader? = null
    private var imageLoaderFactory: ImageLoaderFactory? = null

    /**
     * Get the singleton [ImageLoader].
     */
    @JvmStatic
    fun imageLoader(context: Context): ImageLoader {
        return imageLoader ?: newImageLoader(context)
    }

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

    /**
     * Clear the [ImageLoader] and [ImageLoaderFactory] held by this class.
     *
     * This method is useful for testing and its use is discouraged in production code.
     */
    @JvmStatic
    @Synchronized
    fun reset() {
        imageLoader = null
        imageLoaderFactory = null
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

    @Deprecated(
        message = "Replace with 'context.imageLoader.enqueue(request)'.",
        replaceWith = ReplaceWith(
            expression = "request.context.imageLoader.enqueue(request)",
            imports = ["coil.imageLoader"]
        ),
        level = DeprecationLevel.ERROR // Temporary migration aid.
    )
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun enqueue(request: ImageRequest): Disposable = error("Unsupported")

    @Deprecated(
        message = "Replace with 'context.imageLoader.execute(request)'.",
        replaceWith = ReplaceWith(
            expression = "request.context.imageLoader.execute(request)",
            imports = ["coil.imageLoader"]
        ),
        level = DeprecationLevel.ERROR // Temporary migration aid.
    )
    @JvmStatic
    @Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER")
    suspend fun execute(request: ImageRequest): ImageResult = error("Unsupported")
}
