package coil

import kotlin.jvm.JvmStatic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * A class that holds the singleton [ImageLoader] instance.
 */
object Coil {
    private val lock = SynchronizedObject()
    private var imageLoader: ImageLoader? = null
    private var imageLoaderFactory: ImageLoaderFactory? = null

    /**
     * Get the singleton [ImageLoader].
     */
    @JvmStatic
    fun imageLoader(context: PlatformContext): ImageLoader {
        return imageLoader ?: newImageLoader(context)
    }

    /**
     * Set the singleton [ImageLoader].
     * Prefer using `setImageLoader(ImageLoaderFactory)` to create the [ImageLoader] lazily.
     */
    @JvmStatic
    fun setImageLoader(imageLoader: ImageLoader) = synchronized(lock) {
        this.imageLoaderFactory = null
        this.imageLoader = imageLoader
    }

    /**
     * Set the [ImageLoaderFactory] that will be used to create the singleton [ImageLoader].
     * The [factory] is guaranteed to be called at most once.
     */
    @JvmStatic
    fun setImageLoader(factory: ImageLoaderFactory) = synchronized(lock) {
        imageLoaderFactory = factory
        imageLoader = null
    }

    /**
     * Clear the [ImageLoader] and [ImageLoaderFactory] held by this class.
     *
     * This method is useful for testing and its use is discouraged in production code.
     */
    @JvmStatic
    fun reset() = synchronized(lock) {
        imageLoader = null
        imageLoaderFactory = null
    }

    /** Create and set the new singleton [ImageLoader]. */
    private fun newImageLoader(context: PlatformContext): ImageLoader = synchronized(lock) {
        // Check again in case imageLoader was just set.
        imageLoader?.let { return it }

        // Create a new ImageLoader.
        val newImageLoader = imageLoaderFactory?.newImageLoader()
            ?: context.applicationImageLoaderFactory()?.newImageLoader()
            ?: ImageLoader.Builder(context).build()
        imageLoaderFactory = null
        imageLoader = newImageLoader
        return newImageLoader
    }
}

internal expect fun PlatformContext.applicationImageLoaderFactory(): ImageLoaderFactory?
