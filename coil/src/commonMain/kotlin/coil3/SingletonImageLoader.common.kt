package coil3

import coil3.SingletonImageLoader.Factory
import kotlin.jvm.JvmStatic
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.atomicfu.updateAndGet

/**
 * A class that holds the singleton [ImageLoader] instance.
 */
object SingletonImageLoader {

    private val reference = atomic<Any?>(null)

    /**
     * Get the singleton [ImageLoader].
     */
    @JvmStatic
    fun get(context: PlatformContext): ImageLoader {
        return (reference.value as? ImageLoader) ?: newImageLoader(context)
    }

    /**
     * Set the [Factory] that will be used to lazily create the singleton [ImageLoader].
     *
     * - This must be called before [get] is invoked or [factory] **will not** be set.
     * - If an [ImageLoader] or [Factory] has already been set it **will not** be replaced.
     * - It's safe to call [set] multiple times.
     *
     * The factory is guaranteed to be invoked at most once.
     */
    @JvmStatic
    fun set(factory: Factory) {
        if (reference.value != null) return

        reference.update { value ->
            value ?: factory
        }
    }

    /**
     * Set the singleton [ImageLoader] and overwrite any previously set value.
     */
    @JvmStatic
    fun replace(imageLoader: ImageLoader) {
        reference.value = imageLoader
    }

    /**
     * Set the [Factory] that will be used to lazily create the singleton [ImageLoader] and
     * overwrite any previously set value.
     *
     * The factory is guaranteed to be invoked at most once.
     */
    @JvmStatic
    fun replace(factory: Factory) {
        reference.value = factory
    }

    /**
     * Clear the [ImageLoader] or [Factory] held by this class.
     */
    @JvmStatic
    fun reset() {
        reference.value = null
    }

    @Deprecated(
        message = "set has been renamed to replace.",
        replaceWith = ReplaceWith("replace(imageLoader)"),
    )
    @JvmStatic
    fun set(imageLoader: ImageLoader) = replace(imageLoader)

    /**
     * Create and set the new singleton [ImageLoader].
     */
    private fun newImageLoader(context: PlatformContext): ImageLoader {
        // Local storage to ensure newImageLoader is invoked at most once.
        var imageLoader: ImageLoader? = null

        return reference.updateAndGet { value ->
            when {
                value is ImageLoader -> value
                imageLoader != null -> imageLoader
                else -> {
                    ((value as? Factory)?.newImageLoader(context)
                        ?: context.applicationImageLoaderFactory()?.newImageLoader(context)
                        ?: DefaultSingletonImageLoaderFactory.newImageLoader(context))
                        .also { imageLoader = it }
                }
            }
        } as ImageLoader
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
        fun newImageLoader(context: PlatformContext): ImageLoader
    }
}

internal expect fun PlatformContext.applicationImageLoaderFactory(): Factory?

private val DefaultSingletonImageLoaderFactory = Factory(::ImageLoader)
