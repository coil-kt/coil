package coil3.request

import coil3.ComponentRegistry
import coil3.Extras
import coil3.Image
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.annotation.Data
import coil3.annotation.MainThread
import coil3.decode.Decoder
import coil3.fetch.Fetcher
import coil3.memory.MemoryCache
import coil3.orEmpty
import coil3.request.ImageRequest.Builder
import coil3.size.Dimension
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import coil3.size.SizeResolver
import coil3.target.Target
import coil3.util.EMPTY_IMAGE_FACTORY
import coil3.util.allowInexactSize
import coil3.util.defaultFileSystem
import coil3.util.ioCoroutineDispatcher
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okio.FileSystem

/**
 * An immutable value object that represents a request for an image.
 *
 * @see ImageLoader.enqueue
 * @see ImageLoader.execute
 */
@Data
class ImageRequest private constructor(
    val context: PlatformContext,

    /** @see Builder.data */
    val data: Any,

    /** @see Builder.target */
    val target: Target?,

    /** @see Builder.listener */
    val listener: Listener?,

    /** @see Builder.memoryCacheKey */
    val memoryCacheKey: String?,

    /** @see Builder.memoryCacheKeyExtra */
    val memoryCacheKeyExtras: Map<String, String>,

    /** @see Builder.diskCacheKey */
    val diskCacheKey: String?,

    /** @see Builder.fileSystem */
    val fileSystem: FileSystem,

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
    val placeholderFactory: (ImageRequest) -> Image?,

    /** @see Builder.error */
    val errorFactory: (ImageRequest) -> Image?,

    /** @see Builder.fallback */
    val fallbackFactory: (ImageRequest) -> Image?,

    /** @see Builder.size */
    val sizeResolver: SizeResolver,

    /** @see Builder.scale */
    val scale: Scale,

    /** @see Builder.precision */
    val precision: Precision,

    /** @see Builder.extras */
    val extras: Extras,

    /** The raw values set on [Builder]. */
    val defined: Defined,

    /** The defaults used to fill unset values. */
    val defaults: Defaults,
) {

    /** Create and return a new placeholder image. */
    fun placeholder(): Image? {
        return placeholderFactory(this) ?: defaults.placeholderFactory(this)
    }

    /** Create and return a new error image. */
    fun error(): Image? {
        return errorFactory(this) ?: defaults.errorFactory(this)
    }

    /** Create and return a new fallback image. */
    fun fallback(): Image? {
        return fallbackFactory(this) ?: defaults.fallbackFactory(this)
    }

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
    @Data
    class Defined(
        val interceptorDispatcher: CoroutineDispatcher?,
        val fetcherDispatcher: CoroutineDispatcher?,
        val decoderDispatcher: CoroutineDispatcher?,
        val memoryCachePolicy: CachePolicy?,
        val diskCachePolicy: CachePolicy?,
        val networkCachePolicy: CachePolicy?,
        val placeholderFactory: ((ImageRequest) -> Image?)?,
        val errorFactory: ((ImageRequest) -> Image?)?,
        val fallbackFactory: ((ImageRequest) -> Image?)?,
        val sizeResolver: SizeResolver?,
        val scale: Scale?,
        val precision: Precision?,
    ) {

        fun copy(
            interceptorDispatcher: CoroutineDispatcher? = this.interceptorDispatcher,
            fetcherDispatcher: CoroutineDispatcher? = this.fetcherDispatcher,
            decoderDispatcher: CoroutineDispatcher? = this.decoderDispatcher,
            memoryCachePolicy: CachePolicy? = this.memoryCachePolicy,
            diskCachePolicy: CachePolicy? = this.diskCachePolicy,
            networkCachePolicy: CachePolicy? = this.networkCachePolicy,
            placeholderFactory: ((ImageRequest) -> Image?)? = this.placeholderFactory,
            errorFactory: ((ImageRequest) -> Image?)? = this.errorFactory,
            fallbackFactory: ((ImageRequest) -> Image?)? = this.fallbackFactory,
            sizeResolver: SizeResolver? = this.sizeResolver,
            scale: Scale? = this.scale,
            precision: Precision? = this.precision,
        ) = Defined(
            interceptorDispatcher = interceptorDispatcher,
            fetcherDispatcher = fetcherDispatcher,
            decoderDispatcher = decoderDispatcher,
            memoryCachePolicy = memoryCachePolicy,
            diskCachePolicy = diskCachePolicy,
            networkCachePolicy = networkCachePolicy,
            placeholderFactory = placeholderFactory,
            errorFactory = errorFactory,
            fallbackFactory = fallbackFactory,
            sizeResolver = sizeResolver,
            scale = scale,
            precision = precision,
        )
    }

    /**
     * A set of default options that are used to fill in unset [ImageRequest] values.
     */
    @Data
    class Defaults(
        val fileSystem: FileSystem = defaultFileSystem(),
        val interceptorDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
        val fetcherDispatcher: CoroutineDispatcher = ioCoroutineDispatcher(),
        val decoderDispatcher: CoroutineDispatcher = ioCoroutineDispatcher(),
        val memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,
        val diskCachePolicy: CachePolicy = CachePolicy.ENABLED,
        val networkCachePolicy: CachePolicy = CachePolicy.ENABLED,
        val placeholderFactory: (ImageRequest) -> Image? = EMPTY_IMAGE_FACTORY,
        val errorFactory: (ImageRequest) -> Image? = EMPTY_IMAGE_FACTORY,
        val fallbackFactory: (ImageRequest) -> Image? = EMPTY_IMAGE_FACTORY,
        val precision: Precision = Precision.AUTOMATIC,
        val extras: Extras = Extras.EMPTY,
    ) {

        fun copy(
            fileSystem: FileSystem = this.fileSystem,
            interceptorDispatcher: CoroutineDispatcher = this.interceptorDispatcher,
            fetcherDispatcher: CoroutineDispatcher = this.fetcherDispatcher,
            decoderDispatcher: CoroutineDispatcher = this.decoderDispatcher,
            memoryCachePolicy: CachePolicy = this.memoryCachePolicy,
            diskCachePolicy: CachePolicy = this.diskCachePolicy,
            networkCachePolicy: CachePolicy = this.networkCachePolicy,
            placeholderFactory: (ImageRequest) -> Image? = this.placeholderFactory,
            errorFactory: (ImageRequest) -> Image? = this.errorFactory,
            fallbackFactory: (ImageRequest) -> Image? = this.fallbackFactory,
            precision: Precision = this.precision,
            extras: Extras = this.extras,
        ) = Defaults(
            fileSystem = fileSystem,
            interceptorDispatcher = interceptorDispatcher,
            fetcherDispatcher = fetcherDispatcher,
            decoderDispatcher = decoderDispatcher,
            memoryCachePolicy = memoryCachePolicy,
            diskCachePolicy = diskCachePolicy,
            networkCachePolicy = networkCachePolicy,
            placeholderFactory = placeholderFactory,
            errorFactory = errorFactory,
            fallbackFactory = fallbackFactory,
            precision = precision,
            extras = extras,
        )

        companion object {
            @JvmField val DEFAULT = Defaults()
        }
    }

    class Builder {

        internal val context: PlatformContext
        internal var defaults: Defaults
        internal var data: Any?
        internal var target: Target?
        internal var listener: Listener?
        internal var memoryCacheKey: String?
        internal var lazyMemoryCacheKeyExtras: MutableMap<String, String>?
        internal val memoryCacheKeyExtras: MutableMap<String, String>
            get() = lazyMemoryCacheKeyExtras ?: mutableMapOf<String, String>()
                .also { lazyMemoryCacheKeyExtras = it }
        internal var diskCacheKey: String?
        internal var fileSystem: FileSystem?
        internal var fetcherFactory: Pair<Fetcher.Factory<*>, KClass<*>>?
        internal var decoderFactory: Decoder.Factory?
        internal var interceptorDispatcher: CoroutineDispatcher?
        internal var fetcherDispatcher: CoroutineDispatcher?
        internal var decoderDispatcher: CoroutineDispatcher?
        internal var memoryCachePolicy: CachePolicy?
        internal var diskCachePolicy: CachePolicy?
        internal var networkCachePolicy: CachePolicy?
        internal var placeholderMemoryCacheKey: MemoryCache.Key?
        internal var placeholderFactory: ((ImageRequest) -> Image?)?
        internal var errorFactory: ((ImageRequest) -> Image?)?
        internal var fallbackFactory: ((ImageRequest) -> Image?)?
        internal var precision: Precision?
        internal var lazyExtras: Extras.Builder?
        val extras: Extras.Builder
            get() = lazyExtras ?: Extras.Builder().also { lazyExtras = it }

        internal var sizeResolver: SizeResolver?
        internal var scale: Scale?
        internal var resolvedSizeResolver: SizeResolver?
        internal var resolvedScale: Scale?

        constructor(context: PlatformContext) {
            this.context = context
            defaults = Defaults.DEFAULT
            data = null
            target = null
            listener = null
            memoryCacheKey = null
            lazyMemoryCacheKeyExtras = null
            diskCacheKey = null
            fileSystem = null
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
            lazyExtras = null

            sizeResolver = null
            scale = null
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
            lazyMemoryCacheKeyExtras = if (request.memoryCacheKeyExtras.isEmpty()) {
                null
            } else {
                request.memoryCacheKeyExtras.toMutableMap()
            }
            diskCacheKey = request.diskCacheKey
            fileSystem = request.fileSystem
            fetcherFactory = request.fetcherFactory
            decoderFactory = request.decoderFactory
            interceptorDispatcher = request.defined.interceptorDispatcher
            fetcherDispatcher = request.defined.fetcherDispatcher
            decoderDispatcher = request.defined.decoderDispatcher
            memoryCachePolicy = request.defined.memoryCachePolicy
            diskCachePolicy = request.defined.diskCachePolicy
            networkCachePolicy = request.defined.networkCachePolicy
            placeholderMemoryCacheKey = request.placeholderMemoryCacheKey
            placeholderFactory = request.defined.placeholderFactory
            errorFactory = request.defined.errorFactory
            fallbackFactory = request.defined.fallbackFactory
            precision = request.defined.precision
            lazyExtras = if (request.extras.asMap().isEmpty()) {
                null
            } else {
                request.extras.newBuilder()
            }

            sizeResolver = request.defined.sizeResolver
            scale = request.defined.scale

            // If the context changes, recompute the resolved values.
            if (request.context === context) {
                resolvedSizeResolver = request.sizeResolver
                resolvedScale = request.scale
            } else {
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
         * Set the memory cache key for this request.
         *
         * If this is null or is not set, the [ImageLoader] will compute a memory cache key.
         */
        fun memoryCacheKey(key: MemoryCache.Key?) = apply {
            memoryCacheKey(key?.key)
            memoryCacheKeyExtras(key?.extras.orEmpty())
        }

        /**
         * Set the memory cache key for this request.
         *
         * If this is null or is not set, the [ImageLoader] will compute a memory cache key.
         */
        fun memoryCacheKey(key: String?) = apply {
            this.memoryCacheKey = key
        }

        /**
         * Set extra values to be added to this image request's memory cache key.
         */
        fun memoryCacheKeyExtras(extras: Map<String, String>) = apply {
            this.lazyMemoryCacheKeyExtras = if (extras.isEmpty()) {
                null
            } else {
                extras.toMutableMap()
            }
        }

        /**
         * Set extra values to be added to this image request's memory cache key.
         */
        fun memoryCacheKeyExtra(key: String, value: String?) = apply {
            if (value != null) {
                this.memoryCacheKeyExtras[key] = value
            } else {
                this.lazyMemoryCacheKeyExtras?.remove(key)
            }
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
         * The [FileSystem] that will be used to perform any disk read/write operations.
         */
        fun fileSystem(fileSystem: FileSystem) = apply {
            this.fileSystem = fileSystem
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
        fun placeholder(image: Image?) = placeholder { image }

        /**
         * Set the placeholder image to use when the request starts.
         */
        fun placeholder(factory: (ImageRequest) -> Image?) = apply {
            this.placeholderFactory = factory
        }

        /**
         * Set the error image to use if the request fails.
         */
        fun error(image: Image?) = error { image }

        /**
         * Set the error image to use if the request fails.
         */
        fun error(factory: (ImageRequest) -> Image?) = apply {
            this.errorFactory = factory
        }

        /**
         * Set the fallback image to use if [data] is null.
         */
        fun fallback(image: Image?) = fallback { image }

        /**
         * Set the fallback image to use if [data] is null.
         */
        fun fallback(factory: (ImageRequest) -> Image?) = apply {
            this.fallbackFactory = factory
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
                memoryCacheKeyExtras = lazyMemoryCacheKeyExtras?.toMap().orEmpty(),
                diskCacheKey = diskCacheKey,
                fileSystem = fileSystem ?: defaults.fileSystem,
                fetcherFactory = fetcherFactory,
                decoderFactory = decoderFactory,
                memoryCachePolicy = memoryCachePolicy ?: defaults.memoryCachePolicy,
                diskCachePolicy = diskCachePolicy ?: defaults.diskCachePolicy,
                networkCachePolicy = networkCachePolicy ?: defaults.networkCachePolicy,
                interceptorDispatcher = interceptorDispatcher ?: defaults.interceptorDispatcher,
                fetcherDispatcher = fetcherDispatcher ?: defaults.fetcherDispatcher,
                decoderDispatcher = decoderDispatcher ?: defaults.decoderDispatcher,
                placeholderMemoryCacheKey = placeholderMemoryCacheKey,
                placeholderFactory = placeholderFactory ?: defaults.placeholderFactory,
                errorFactory = errorFactory ?: defaults.errorFactory,
                fallbackFactory = fallbackFactory ?: defaults.fallbackFactory,
                sizeResolver = sizeResolver ?: resolvedSizeResolver ?: resolveSizeResolver(),
                scale = scale ?: resolvedScale ?: resolveScale(),
                precision = precision ?: defaults.precision,
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
                extras = lazyExtras?.build().orEmpty(),
            )
        }

        /** Ensure the size resolver and scale will be recomputed when [build] is called. */
        private fun resetResolvedValues() {
            resolvedSizeResolver = null
            resolvedScale = null
        }

        /** Ensure the scale will be recomputed when [build] is called. */
        private fun resetResolvedScale() {
            resolvedScale = null
        }
    }
}

internal expect fun Builder.resolveSizeResolver(): SizeResolver

internal expect fun Builder.resolveScale(): Scale
