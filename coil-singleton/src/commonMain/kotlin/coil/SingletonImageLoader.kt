package coil

import kotlin.jvm.JvmStatic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * A class that holds the singleton [ImageLoader] instance.
 */
object SingletonImageLoader {
    private val lock = SynchronizedObject()
    private var imageLoader: ImageLoader? = null
    private var imageLoaderFactory: Factory? = null

    /**
     * Get the singleton [ImageLoader].
     */
    @JvmStatic
    fun get(context: PlatformContext): ImageLoader {
        return imageLoader ?: newImageLoader(context)
    }

    /**
     * Set the singleton [ImageLoader]. Prefer using `set(Factory)` to create the
     * [ImageLoader] lazily.
     */
    @JvmStatic
    fun set(imageLoader: ImageLoader) = synchronized(lock) {
        this.imageLoaderFactory = null
        this.imageLoader = imageLoader
    }

    /**
     * Set the [SingletonImageLoader.Factory] that will be used to create the singleton
     * [ImageLoader]. The [factory] is guaranteed to be called at most once.
     */
    @JvmStatic
    fun set(factory: Factory) = synchronized(lock) {
        imageLoaderFactory = factory
        imageLoader = null
    }

    /**
     * Clear the [ImageLoader] and [SingletonImageLoader.Factory] held by this class.
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

    /**
     * A factory that creates the new singleton [ImageLoader].
     *
     * To configure how the singleton [ImageLoader] is created **either**:
     * - Implement [SingletonImageLoader.Factory] on your Android `Application` class.
     * - **Or** call [SingletonImageLoader.set] with your [SingletonImageLoader.Factory].
     */
    fun interface Factory {

        /** Return a new [ImageLoader]. */
        fun newImageLoader(): ImageLoader
    }
}

internal expect fun PlatformContext.applicationImageLoaderFactory(): SingletonImageLoader.Factory?
