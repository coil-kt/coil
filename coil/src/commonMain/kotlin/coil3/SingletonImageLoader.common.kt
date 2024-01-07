package coil3

import coil3.SingletonImageLoader.Factory
import coil3.annotation.DelicateCoilApi
import kotlin.jvm.JvmStatic
import kotlinx.atomicfu.atomic
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
     * This function is similar to [setUnsafe] except:
     *
     * - If an [ImageLoader] has already been created it **will not** be replaced with [factory].
     * - If the default [ImageLoader] has already been created, an error will be thrown as it
     *   indicates [setSafe] is being called too late and after [get] has already been called.
     * - It's safe to call [setSafe] multiple times.
     *
     * The factory is guaranteed to be invoked at most once.
     */
    @JvmStatic
    fun setSafe(factory: Factory) {
        val value = reference.value
        if (value is ImageLoader) {
            if (value.isDefault) {
                error(
                    """The default image loader has already been created. This indicates that
                    'setSafe' is being called after the first 'get' call. Ensure that 'setSafe' is
                    called before any Coil API usages (e.g. `load`, `AsyncImage`,
                    `rememberAsyncImagePainter`, etc.).
                    """.trimIndent(),
                )
            }
            return
        }

        reference.compareAndSet(value, factory)
    }

    /**
     * Set the singleton [ImageLoader] and overwrite any previously set value.
     */
    @DelicateCoilApi
    @JvmStatic
    fun setUnsafe(imageLoader: ImageLoader) {
        reference.value = imageLoader
    }

    /**
     * Set the [Factory] that will be used to lazily create the singleton [ImageLoader] and
     * overwrite any previously set value.
     *
     * The factory is guaranteed to be invoked at most once.
     */
    @DelicateCoilApi
    @JvmStatic
    fun setUnsafe(factory: Factory) {
        reference.value = factory
    }

    /**
     * Clear the [ImageLoader] or [Factory] held by this class.
     */
    @DelicateCoilApi
    @JvmStatic
    fun reset() {
        reference.value = null
    }

    @Deprecated(
        message = "'set' has been renamed to 'setUnsafe'.",
        replaceWith = ReplaceWith("setUnsafe(imageLoader)"),
    )
    @JvmStatic
    fun set(imageLoader: ImageLoader) = setUnsafe(imageLoader)

    @Deprecated(
        message = "'set' has been renamed to 'setUnsafe'.",
        replaceWith = ReplaceWith("setUnsafe(factory)"),
    )
    @JvmStatic
    fun set(factory: Factory) = setUnsafe(factory)

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
     * - **Or** call [SingletonImageLoader.setSafe] with your [SingletonImageLoader.Factory].
     */
    fun interface Factory {

        /** Return a new [ImageLoader]. */
        fun newImageLoader(context: PlatformContext): ImageLoader
    }
}

internal expect fun PlatformContext.applicationImageLoaderFactory(): Factory?

private val DefaultSingletonImageLoaderFactory = Factory { context ->
    ImageLoader.Builder(context)
        // Add a marker value so we know this was created by the default singleton image loader.
        .apply { extras[DefaultSingletonImageLoaderKey] = Unit }
        .build()
}

private val ImageLoader.isDefault: Boolean
    get() = defaults.extras[DefaultSingletonImageLoaderKey] != null

private val DefaultSingletonImageLoaderKey = Extras.Key(Unit)
