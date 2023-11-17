package coil.request

import coil.ComponentRegistry
import coil.Extras
import coil.Image
import coil.ImageLoader
import coil.PlatformContext
import coil.annotation.MainThread
import coil.decode.Decoder
import coil.fetch.Fetcher
import coil.memory.MemoryCache
import coil.request.ImageRequest.Builder
import coil.size.Dimension
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import coil.target.Target
import coil.util.EMPTY_IMAGE_FACTORY
import coil.util.allowInexactSize
import coil.util.ioCoroutineDispatcher
import dev.drewhamilton.poko.Poko
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * An immutable value object that represents a request for an image.
 *
 * @see ImageLoader.enqueue
 * @see ImageLoader.execute
 */
@Poko
class ImageRequest private constructor(
    val context: PlatformContext,

    /** @see Builder.data */
    val data: Any,

    /** @see Builder.target */
    val target: Target?,

    /** @see Builder.listener */
    val listener: Listener?,

    /** @see Builder.memoryCacheKey */
    val memoryCacheKey: MemoryCache.Key?,

    /** @see Builder.diskCacheKey */
    val diskCacheKey: String?,

    /** @see Builder.fetcherFactory */
    val fetcherFactory: Pair<Fetcher.Factory<*>, KClass<*>>?,

    /** @see Builder.decoderFactory */
    val decoderFactory: Decoder.Factory?,

    /** @see Builder.interceptorDispatcher */
    val interceptorDispatcher: CoroutineDispatcher,

    /** @see Builder.fetcherDispatcher */
    val fetcherDispatcher: CoroutineDispatcher,

    /** @see Builder.decoderDispatcher */
    val decoderDispatcher: CoroutineDispatcher,

    /** @see Builder.memoryCachePolicy */
    val memoryCachePolicy: CachePolicy,

    /** @see Builder.diskCachePolicy */
    val diskCachePolicy: CachePolicy,

    /** @see Builder.networkCachePolicy */
    val networkCachePolicy: CachePolicy,

    /** @see Builder.placeholderMemoryCacheKey */
    val placeholderMemoryCacheKey: MemoryCache.Key?,

    /** @see Builder.placeholder */
    val placeholderFactory: () -> Image?,

    /** @see Builder.error */
    val errorFactory: () -> Image?,

    /** @see Builder.fallback */
    val fallbackFactory: () -> Image?,

    /** @see Builder.size */
    val sizeResolver: SizeResolver,

    /** @see Builder.scale */
    val scale: Scale,

    /** @see Builder.precision */
    val precision: Precision,

    /** @see Builder.extra */
    val extras: Extras,

    /** The raw values set on [Builder]. */
    val defined: Defined,

    /** The defaults used to fill unset values. */
    val defaults: Defaults,
) {

    @JvmOverloads
    fun newBuilder(
        context: PlatformContext = this.context,
    ) = Builder(this, context)

    /**
     * A set of callbacks for an [ImageRequest].
     */
    interface Listener {

        /**
         * Called immediately after [Target.onStart].
         */
        @MainThread
        fun onStart(request: ImageRequest) {}

        /**
         * Called if the request is cancelled.
         */
        @MainThread
        fun onCancel(request: ImageRequest) {}

        /**
         * Called if an error occurs while executing the request.
         */
        @MainThread
        fun onError(request: ImageRequest, result: ErrorResult) {}

        /**
         * Called if the request completes successfully.
         */
        @MainThread
        fun onSuccess(request: ImageRequest, result: SuccessResult) {}
    }

    /**
     * Tracks which values have been set (instead of computed automatically using a default)
     * when building an [ImageRequest].
     */
    data class Defined(
        val interceptorDispatcher: CoroutineDispatcher?,
        val fetcherDispatcher: CoroutineDispatcher?,
        val decoderDispatcher: CoroutineDispatcher?,
        val memoryCachePolicy: CachePolicy?,
        val diskCachePolicy: CachePolicy?,
        val networkCachePolicy: CachePolicy?,
        val placeholderFactory: () -> Image?,
        val errorFactory: () -> Image?,
        val fallbackFactory: () -> Image?,
        val sizeResolver: SizeResolver?,
        val scale: Scale?,
        val precision: Precision?,
        val extras: Extras,
    )

    /**
     * A set of default options that are used to fill in unset [ImageRequest] values.
     */
    data class Defaults(
        val interceptorDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
        val fetcherDispatcher: CoroutineDispatcher = ioCoroutineDispatcher(),
        val decoderDispatcher: CoroutineDispatcher = ioCoroutineDispatcher(),
        val memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,
        val diskCachePolicy: CachePolicy = CachePolicy.ENABLED,
        val networkCachePolicy: CachePolicy = CachePolicy.ENABLED,
        val placeholderFactory: () -> Image? = EMPTY_IMAGE_FACTORY,
        val errorFactory: () -> Image? = EMPTY_IMAGE_FACTORY,
        val fallbackFactory: () -> Image? = EMPTY_IMAGE_FACTORY,
        val precision: Precision = Precision.AUTOMATIC,
        val extras: Extras = Extras.EMPTY,
    ) {
        companion object {
            @JvmField val DEFAULT = Defaults()
        }
    }

    class Builder {

        private val context: PlatformContext
        private var defaults: Defaults
        private var data: Any?
        private var target: Target?
        private var listener: Listener?
        private var memoryCacheKey: MemoryCache.Key?
        private var diskCacheKey: String?
        private var fetcherFactory: Pair<Fetcher.Factory<*>, KClass<*>>?
        private var decoderFactory: Decoder.Factory?
        private var interceptorDispatcher: CoroutineDispatcher?
        private var fetcherDispatcher: CoroutineDispatcher?
        private var decoderDispatcher: CoroutineDispatcher?
        private var memoryCachePolicy: CachePolicy?
        private var diskCachePolicy: CachePolicy?
        private var networkCachePolicy: CachePolicy?
        private var placeholderMemoryCacheKey: MemoryCache.Key?
        private var placeholderFactory: () -> Image?
        private var errorFactory: () -> Image?
        private var fallbackFactory: () -> Image?
        private var precision: Precision?
        internal val extras: Extras.Builder
        internal val resolvedExtras: Extras.Builder

        private var lifecycle: Lifecycle?
        private var sizeResolver: SizeResolver?
        private var scale: Scale?
        private var resolvedLifecycle: Lifecycle?
        private var resolvedSizeResolver: SizeResolver?
        private var resolvedScale: Scale?

        constructor(context: PlatformContext) {
            this.context = context
            defaults = Defaults.DEFAULT
            data = null
            target = null
            listener = null
            memoryCacheKey = null
            diskCacheKey = null
            fetcherFactory = null
            decoderFactory = null
            interceptorDispatcher = null
            fetcherDispatcher = null
            decoderDispatcher = null
            memoryCachePolicy = null
            diskCachePolicy = null
            networkCachePolicy = null
            placeholderMemoryCacheKey = null
            placeholderFactory = EMPTY_IMAGE_FACTORY
            errorFactory = EMPTY_IMAGE_FACTORY
            fallbackFactory = EMPTY_IMAGE_FACTORY
            precision = null
            extras = Extras.Builder()
            resolvedExtras = Extras.Builder()

            lifecycle = null
            sizeResolver = null
            scale = null
            resolvedLifecycle = null
            resolvedSizeResolver = null
            resolvedScale = null
        }

        @JvmOverloads
        constructor(request: ImageRequest, context: PlatformContext = request.context) {
            this.context = context
            defaults = request.defaults
            data = request.data
            target = request.target
            listener = request.listener
            memoryCacheKey = request.memoryCacheKey
            diskCacheKey = request.diskCacheKey
            fetcherFactory = request.fetcherFactory
            decoderFactory = request.decoderFactory
            interceptorDispatcher = request.defined.interceptorDispatcher
            fetcherDispatcher = request.defined.fetcherDispatcher
            decoderDispatcher = request.defined.decoderDispatcher
            memoryCachePolicy = request.defined.memoryCachePolicy
            diskCachePolicy = request.defined.diskCachePolicy
            networkCachePolicy = request.defined.networkCachePolicy
            placeholderMemoryCacheKey = request.placeholderMemoryCacheKey
            placeholderFactory = EMPTY_IMAGE_FACTORY
            errorFactory = EMPTY_IMAGE_FACTORY
            fallbackFactory = EMPTY_IMAGE_FACTORY
            precision = request.defined.precision
            extras = request.extras.newBuilder()

            lifecycle = request.defined.lifecycle
            sizeResolver = request.defined.sizeResolver
            scale = request.defined.scale

            // If the context changes, recompute the resolved values.
            if (request.context === context) {
                resolvedLifecycle = request.lifecycle
                resolvedSizeResolver = request.sizeResolver
                resolvedScale = request.scale
            } else {
                resolvedLifecycle = null
                resolvedSizeResolver = null
                resolvedScale = null
            }
        }

        /**
         * Set the data to load.
         */
        fun data(data: Any?) = apply {
            this.data = data
        }

        /**
         * Set the memory cache key for this request.
         *
         * If this is null or is not set, the [ImageLoader] will compute a memory cache key.
         */
        fun memoryCacheKey(key: String?) = memoryCacheKey(key?.let { MemoryCache.Key(it) })

        /**
         * Set the memory cache key for this request.
         *
         * If this is null or is not set, the [ImageLoader] will compute a memory cache key.
         */
        fun memoryCacheKey(key: MemoryCache.Key?) = apply {
            this.memoryCacheKey = key
        }

        /**
         * Set the disk cache key for this request.
         *
         * If this is null or is not set, the [ImageLoader] will compute a disk cache key.
         */
        fun diskCacheKey(key: String?) = apply {
            this.diskCacheKey = key
        }

        /**
         * Convenience function to create and set the [Listener].
         */
        inline fun listener(
            crossinline onStart: (request: ImageRequest) -> Unit = {},
            crossinline onCancel: (request: ImageRequest) -> Unit = {},
            crossinline onError: (request: ImageRequest, result: ErrorResult) -> Unit = { _, _ -> },
            crossinline onSuccess: (request: ImageRequest, result: SuccessResult) -> Unit = { _, _ -> }
        ) = listener(object : Listener {
            override fun onStart(request: ImageRequest) = onStart(request)
            override fun onCancel(request: ImageRequest) = onCancel(request)
            override fun onError(request: ImageRequest, result: ErrorResult) = onError(request, result)
            override fun onSuccess(request: ImageRequest, result: SuccessResult) = onSuccess(request, result)
        })

        /**
         * Set the [Listener].
         */
        fun listener(listener: Listener?) = apply {
            this.listener = listener
        }

        /**
         * @see ImageLoader.Builder.dispatcher
         */
        fun dispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.fetcherDispatcher = dispatcher
            this.decoderDispatcher = dispatcher
        }

        /**
         * @see ImageLoader.Builder.interceptorDispatcher
         */
        fun interceptorDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.interceptorDispatcher = dispatcher
        }

        /**
         * @see ImageLoader.Builder.fetcherDispatcher
         */
        fun fetcherDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.fetcherDispatcher = dispatcher
        }

        /**
         * @see ImageLoader.Builder.decoderDispatcher
         */
        fun decoderDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.decoderDispatcher = dispatcher
        }

        /**
         * Set the requested width/height.
         */
        fun size(size: Int) = size(Size(size, size))

        /**
         * Set the requested width/height.
         */
        fun size(width: Int, height: Int) = size(Size(width, height))

        /**
         * Set the requested width/height.
         */
        fun size(width: Dimension, height: Dimension) = size(Size(width, height))

        /**
         * Set the requested width/height.
         */
        fun size(size: Size) = size(SizeResolver(size))

        /**
         * Set the [SizeResolver] to resolve the requested width/height.
         */
        fun size(resolver: SizeResolver) = apply {
            this.sizeResolver = resolver
            resetResolvedValues()
        }

        /**
         * Set the scaling algorithm that will be used to fit/fill the image into the size provided
         * by [sizeResolver].
         *
         * NOTE: If [scale] is not set, it is automatically computed for [ImageView] targets.
         */
        fun scale(scale: Scale) = apply {
            this.scale = scale
        }

        /**
         * Set the precision for the size of the loaded image.
         *
         * The default value is [Precision.AUTOMATIC], which uses the logic in [allowInexactSize]
         * to determine if output image's dimensions must match the input [size] and [scale] exactly.
         *
         * NOTE: If [size] is [Size.ORIGINAL], the returned image's size will always be equal to or
         * greater than the image's original size.
         *
         * @see Precision
         */
        fun precision(precision: Precision) = apply {
            this.precision = precision
        }

        /**
         * Use [factory] to handle fetching any image data.
         *
         * If this is null or is not set the [ImageLoader] will find an applicable fetcher in its
         * [ComponentRegistry].
         */
        inline fun <reified T : Any> fetcherFactory(factory: Fetcher.Factory<T>) =
            fetcherFactory(factory, T::class)

        /**
         * Use [factory] to handle fetching any image data.
         *
         * If this is null or is not set the [ImageLoader] will find an applicable fetcher in its
         * [ComponentRegistry].
         */
        fun <T : Any> fetcherFactory(factory: Fetcher.Factory<T>, type: KClass<T>) = apply {
            this.fetcherFactory = factory to type
        }

        /**
         * Use [factory] to handle decoding any image data.
         *
         * If this is null or is not set the [ImageLoader] will find an applicable decoder in its
         * [ComponentRegistry].
         */
        fun decoderFactory(factory: Decoder.Factory) = apply {
            this.decoderFactory = factory
        }

        /**
         * Enable/disable reading/writing from/to the memory cache.
         */
        fun memoryCachePolicy(policy: CachePolicy) = apply {
            this.memoryCachePolicy = policy
        }

        /**
         * Enable/disable reading/writing from/to the disk cache.
         */
        fun diskCachePolicy(policy: CachePolicy) = apply {
            this.diskCachePolicy = policy
        }

        /**
         * Enable/disable reading from the network.
         *
         * NOTE: Disabling writes has no effect.
         */
        fun networkCachePolicy(policy: CachePolicy) = apply {
            this.networkCachePolicy = policy
        }

        /**
         * Set the memory cache [key] whose value will be used as the placeholder drawable.
         *
         * If there is no value in the memory cache for [key], fall back to [placeholder].
         */
        fun placeholderMemoryCacheKey(key: String?) =
            placeholderMemoryCacheKey(key?.let { MemoryCache.Key(it) })

        /**
         * Set the memory cache [key] whose value will be used as the placeholder image.
         *
         * If there is no value in the memory cache for [key], fall back to [placeholder].
         */
        fun placeholderMemoryCacheKey(key: MemoryCache.Key?) = apply {
            this.placeholderMemoryCacheKey = key
        }

        /**
         * Set the placeholder image to use when the request starts.
         */
        fun placeholder(image: Image) = placeholder { image }

        /**
         * Set the placeholder image to use when the request starts.
         */
        fun placeholder(factory: () -> Image?) = apply {
            this.placeholderFactory = factory
        }

        /**
         * Set the error image to use if the request fails.
         */
        fun error(image: Image) = error { image }

        /**
         * Set the error image to use if the request fails.
         */
        fun error(factory: () -> Image?) = apply {
            this.errorFactory = factory
        }

        /**
         * Set the fallback image to use if [data] is null.
         */
        fun fallback(image: Image) = fallback { image }

        /**
         * Set the fallback image to use if [data] is null.
         */
        fun fallback(factory: () -> Image?) = apply {
            this.fallbackFactory = factory
        }

        /**
         * Convenience function to create and set the [Target].
         */
        inline fun target(
            crossinline onStart: (placeholder: Image?) -> Unit = {},
            crossinline onError: (error: Image?) -> Unit = {},
            crossinline onSuccess: (result: Image) -> Unit = {}
        ) = target(object : Target {
            override fun onStart(placeholder: Image?) = onStart(placeholder)
            override fun onError(error: Image?) = onError(error)
            override fun onSuccess(result: Image) = onSuccess(result)
        })

        /**
         * Set the [Target].
         */
        fun target(target: Target?) = apply {
            this.target = target
            resetResolvedValues()
        }

        /**
         * @see ImageLoader.Builder.crossfade
         */
        fun crossfade(enable: Boolean) =
            crossfade(if (enable) CrossfadeDrawable.DEFAULT_DURATION else 0)

        /**
         * @see ImageLoader.Builder.crossfade
         */
        fun crossfade(durationMillis: Int) = apply {
            val factory = if (durationMillis > 0) {
                CrossfadeTransition.Factory(durationMillis)
            } else {
                Transition.Factory.NONE
            }
            transitionFactory(factory)
        }

        /**
         * @see ImageLoader.Builder.transitionFactory
         */
        fun transitionFactory(transition: Transition.Factory) = apply {
            this.transitionFactory = transition
        }

        fun extra(key: String, value: Any?) = apply {
            extras.put(key, value)
        }

        /**
         * Set the defaults for any unset request values.
         */
        fun defaults(defaults: Defaults) = apply {
            this.defaults = defaults
            resetResolvedScale()
        }

        /**
         * Create a new [ImageRequest].
         */
        fun build(): ImageRequest {
            return ImageRequest(
                context = context,
                data = data ?: NullRequestData,
                target = target,
                listener = listener,
                memoryCacheKey = memoryCacheKey,
                diskCacheKey = diskCacheKey,
                precision = precision ?: defaults.precision,
                fetcherFactory = fetcherFactory,
                decoderFactory = decoderFactory,
                memoryCachePolicy = memoryCachePolicy ?: defaults.memoryCachePolicy,
                diskCachePolicy = diskCachePolicy ?: defaults.diskCachePolicy,
                networkCachePolicy = networkCachePolicy ?: defaults.networkCachePolicy,
                interceptorDispatcher = interceptorDispatcher ?: defaults.interceptorDispatcher,
                fetcherDispatcher = fetcherDispatcher ?: defaults.fetcherDispatcher,
                decoderDispatcher = decoderDispatcher ?: defaults.decoderDispatcher,
                lifecycle = lifecycle ?: resolvedLifecycle ?: resolveLifecycle(),
                sizeResolver = sizeResolver ?: resolvedSizeResolver ?: resolveSizeResolver(),
                scale = scale ?: resolvedScale ?: resolveScale(),
                placeholderMemoryCacheKey = placeholderMemoryCacheKey,
                defined = Defined(
                    interceptorDispatcher = interceptorDispatcher,
                    fetcherDispatcher = fetcherDispatcher,
                    decoderDispatcher = decoderDispatcher,
                    placeholderFactory = placeholderFactory,
                    errorFactory = errorFactory,
                    fallbackFactory = fallbackFactory,
                    memoryCachePolicy = memoryCachePolicy,
                    diskCachePolicy = diskCachePolicy,
                    networkCachePolicy = networkCachePolicy,
                    sizeResolver = sizeResolver,
                    scale = scale,
                    precision = precision,
                ),
                defaults = defaults,
            )
        }

        /** Ensure these values will be recomputed when [build] is called. */
        private fun resetResolvedValues() {
            resolvedLifecycle = null
            resolvedSizeResolver = null
            resolvedScale = null
        }

        /** Ensure the scale will be recomputed when [build] is called. */
        private fun resetResolvedScale() {
            resolvedScale = null
        }

        private fun resolveLifecycle(): Lifecycle {
            val target = target
            val context = if (target is ViewTarget<*>) target.view.context else context
            return context.getLifecycle() ?: GlobalLifecycle
        }

        private fun resolveSizeResolver(): SizeResolver {
            val target = target
            if (target is ViewTarget<*>) {
                // CENTER and MATRIX scale types should be decoded at the image's original size.
                val view = target.view
                if (view is ImageView && view.scaleType.let { it == CENTER || it == MATRIX }) {
                    return SizeResolver(Size.ORIGINAL)
                } else {
                    return ViewSizeResolver(view)
                }
            } else {
                // Fall back to the size of the display.
                return DisplaySizeResolver(context)
            }
        }

        private fun resolveScale(): Scale {
            val view = (sizeResolver as? ViewSizeResolver<*>)?.view ?: (target as? ViewTarget<*>)?.view
            if (view is ImageView) {
                return view.scale
            } else {
                return Scale.FIT
            }
        }
    }
}
