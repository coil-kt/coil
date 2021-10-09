@file:Suppress("unused", "UNUSED_PARAMETER")

package coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.lifecycle.Lifecycle
import coil.decode.Decoder
import coil.disk.DiskCache
import coil.drawable.CrossfadeDrawable
import coil.fetch.Fetcher
import coil.intercept.Interceptor
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.DefaultRequestOptions
import coil.request.Disposable
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.size.Precision
import coil.target.ViewTarget
import coil.transform.Transformation
import coil.transition.CrossfadeTransition
import coil.transition.Transition
import coil.util.DEFAULT_BITMAP_CONFIG
import coil.util.DEFAULT_REQUEST_OPTIONS
import coil.util.ImageLoaderOptions
import coil.util.Logger
import coil.util.Option
import coil.util.Utils
import coil.util.getDrawableCompat
import coil.util.lazyCallFactory
import coil.util.unsupported
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.Call
import okhttp3.OkHttpClient
import java.io.File

/**
 * A service class that loads images by executing [ImageRequest]s. Image loaders handle
 * caching, data fetching, image decoding, request management, memory management, and more.
 *
 * Image loaders are designed to be shareable and work best when you create a single
 * instance and share it throughout your app.
 */
interface ImageLoader {

    /**
     * The default options that are used to fill in unset [ImageRequest] values.
     */
    val defaults: DefaultRequestOptions

    /**
     * The components used to fulfil image requests.
     */
    val components: ComponentRegistry

    /**
     * An in-memory cache of previously loaded images.
     */
    val memoryCache: MemoryCache?

    /**
     * An on-disk cache of previously loaded images.
     */
    val diskCache: DiskCache?

    /**
     * Enqueue the [request] to be executed asynchronously.
     *
     * NOTE: The request will wait until [ImageRequest.lifecycle] is at least
     * [Lifecycle.State.STARTED] before being executed.
     *
     * @param request The request to execute.
     * @return A [Disposable] which can be used to cancel or check the status of the request.
     */
    fun enqueue(request: ImageRequest): Disposable

    /**
     * Execute the [request] in the current coroutine scope.
     *
     * NOTE: If [ImageRequest.target] is a [ViewTarget], the job will automatically be cancelled
     * if its view is detached.
     *
     * @param request The request to execute.
     * @return A [SuccessResult] if the request completes successfully.
     *  Else, returns an [ErrorResult].
     */
    suspend fun execute(request: ImageRequest): ImageResult

    /**
     * Cancel any new and in progress requests, clear the [MemoryCache], and close any open
     * system resources.
     *
     * Shutting down an image loader is optional. It will be shut down automatically if
     * dereferenced.
     */
    fun shutdown()

    /**
     * Create an [ImageLoader.Builder] that shares the same resources and configuration as this
     * image loader.
     */
    fun newBuilder(): Builder

    class Builder {

        private val applicationContext: Context
        private var defaults: DefaultRequestOptions
        private var memoryCache: Option<MemoryCache>?
        private var diskCache: Option<Lazy<DiskCache>>?
        private var callFactory: Call.Factory?
        private var eventListenerFactory: EventListener.Factory?
        private var componentRegistry: ComponentRegistry?
        private var options: ImageLoaderOptions
        private var logger: Logger?

        constructor(context: Context) {
            applicationContext = context.applicationContext
            defaults = DEFAULT_REQUEST_OPTIONS
            memoryCache = null
            diskCache = null
            callFactory = null
            eventListenerFactory = null
            componentRegistry = null
            options = ImageLoaderOptions()
            logger = null
        }

        internal constructor(imageLoader: RealImageLoader) {
            applicationContext = imageLoader.context.applicationContext
            defaults = imageLoader.defaults
            memoryCache = Option(imageLoader.memoryCache)
            diskCache = Option(imageLoader.diskCacheLazy)
            callFactory = imageLoader.callFactory
            eventListenerFactory = imageLoader.eventListenerFactory
            componentRegistry = imageLoader.componentRegistry
            options = imageLoader.options
            logger = imageLoader.logger
        }

        /**
         * Set the [OkHttpClient] used for network requests.
         *
         * This is a convenience function for calling `callFactory(Call.Factory)`.
         */
        fun okHttpClient(okHttpClient: OkHttpClient) = callFactory(okHttpClient)

        /**
         * Set a lazy callback to create the [OkHttpClient] used for network requests.
         *
         * This allows lazy creation of the [OkHttpClient] on a background thread.
         * [initializer] is guaranteed to be called at most once.
         *
         * Prefer using this instead of `okHttpClient(OkHttpClient)`.
         *
         * This is a convenience function for calling `callFactory(() -> Call.Factory)`.
         */
        fun okHttpClient(initializer: () -> OkHttpClient) = callFactory(initializer)

        /**
         * Set the [Call.Factory] used for network requests.
         */
        fun callFactory(callFactory: Call.Factory) = apply {
            this.callFactory = callFactory
        }

        /**
         * Set a lazy callback to create the [Call.Factory] used for network requests.
         *
         * This allows lazy creation of the [Call.Factory] on a background thread.
         * [initializer] is guaranteed to be called at most once.
         *
         * Prefer using this instead of `callFactory(Call.Factory)`.
         */
        fun callFactory(initializer: () -> Call.Factory) = apply {
            this.callFactory = lazyCallFactory(initializer)
        }

        /**
         * Build and set the [ComponentRegistry].
         */
        @JvmSynthetic
        inline fun components(
            builder: ComponentRegistry.Builder.() -> Unit
        ) = components(ComponentRegistry.Builder().apply(builder).build())

        /**
         * Set the [ComponentRegistry].
         */
        fun components(components: ComponentRegistry) = apply {
            this.componentRegistry = components
        }

        /**
         * Set the [MemoryCache].
         */
        fun memoryCache(memoryCache: MemoryCache?) = apply {
            this.memoryCache = Option(memoryCache)
        }

        /**
         * Set the [DiskCache].
         *
         * NOTE: By default, [ImageLoader]s share the same disk cache instance. This is necessary
         * as having multiple disk cache instances active in the same directory at the same time
         * can corrupt the disk cache.
         *
         * @see DiskCache.directory
         */
        fun diskCache(diskCache: DiskCache?) = apply {
            this.diskCache = Option(diskCache?.let(::lazyOf))
        }

        /**
         * Set a lazy callback to create the [DiskCache].
         *
         * Prefer using this instead of `diskCache(DiskCache)`.
         *
         * NOTE: By default, [ImageLoader]s share the same disk cache instance. This is necessary
         * as having multiple disk cache instances active in the same directory at the same time
         * can corrupt the disk cache.
         *
         * @see DiskCache.directory
         */
        fun diskCache(initializer: () -> DiskCache) = apply {
            this.diskCache = Option(lazy(initializer))
        }

        /**
         * Allow the use of [Bitmap.Config.HARDWARE].
         *
         * If false, any use of [Bitmap.Config.HARDWARE] will be treated as
         * [Bitmap.Config.ARGB_8888].
         *
         * NOTE: Setting this to false this will reduce performance on API 26 and above. Only
         * disable this if necessary.
         *
         * Default: true
         */
        fun allowHardware(enable: Boolean) = apply {
            this.defaults = this.defaults.copy(allowHardware = enable)
        }

        /**
         * Allow automatically using [Bitmap.Config.RGB_565] when an image is guaranteed to not
         * have alpha.
         *
         * This will reduce the visual quality of the image, but will also reduce memory usage.
         *
         * Prefer only enabling this for low memory and resource constrained devices.
         *
         * Default: false
         */
        fun allowRgb565(enable: Boolean) = apply {
            this.defaults = this.defaults.copy(allowRgb565 = enable)
        }

        /**
         * Enables adding [File.lastModified] to the memory cache key when loading an image from a
         * [File].
         *
         * This allows subsequent requests that load the same file to miss the memory cache if the
         * file has been updated. However, if the memory cache check occurs on the main thread
         * (see [interceptorDispatcher]) calling [File.lastModified] will cause a strict mode
         * violation.
         *
         * Default: true
         */
        fun addLastModifiedToFileCacheKey(enable: Boolean) = apply {
            this.options = this.options.copy(addLastModifiedToFileCacheKey = enable)
        }

        /**
         * Enables short circuiting network requests if the device is offline.
         *
         * If true, reading from the network will automatically be disabled if the device is
         * offline. If a cached response is unavailable the request will fail with a
         * '504 Unsatisfiable Request' response.
         *
         * If false, the image loader will attempt a network request even if the device is offline.
         *
         * Default: true
         */
        fun networkObserverEnabled(enable: Boolean) = apply {
            this.options = this.options.copy(networkObserverEnabled = enable)
        }

        /**
         * Enables support for network cache headers. If enabled, this image loader will respect the
         * cache headers returned by network responses when deciding if an image can be stored or
         * served from the disk cache. If disabled, images will always be served from the disk cache
         * (if present) and will only be evicted to stay under the maximum size.
         *
         * Default: true
         */
        fun respectCacheHeaders(enable: Boolean) = apply {
            this.options = this.options.copy(respectCacheHeaders = enable)
        }

        /**
         * Sets the maximum number of parallel [BitmapFactory] decode operations at once.
         *
         * Increasing this number will allow more parallel [BitmapFactory] decode operations,
         * however it can result in worse UI performance.
         *
         * Default: 4
         */
        fun bitmapFactoryMaxParallelism(maxParallelism: Int) = apply {
            require(maxParallelism > 0) { "maxParallelism must be > 0." }
            this.options = this.options.copy(bitmapFactoryMaxParallelism = maxParallelism)
        }

        /**
         * Set a single [EventListener] that will receive all callbacks for requests launched by
         * this image loader.
         *
         * @see eventListenerFactory
         */
        fun eventListener(listener: EventListener) = eventListenerFactory { listener }

        /**
         * Set the [EventListener.Factory] to create per-request [EventListener]s.
         */
        fun eventListenerFactory(factory: EventListener.Factory) = apply {
            this.eventListenerFactory = factory
        }

        /**
         * Enable a crossfade animation with duration [CrossfadeDrawable.DEFAULT_DURATION]
         * milliseconds when a request completes successfully.
         *
         * Default: false
         */
        fun crossfade(enable: Boolean) =
            crossfade(if (enable) CrossfadeDrawable.DEFAULT_DURATION else 0)

        /**
         * Enable a crossfade animation with [durationMillis] milliseconds when a request completes
         * successfully.
         *
         * @see `crossfade(Boolean)`
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
         * Set the default [Transition.Factory] for each request.
         */
        fun transitionFactory(factory: Transition.Factory) = apply {
            this.defaults = this.defaults.copy(transitionFactory = factory)
        }

        /**
         * Set the default precision for a request. [Precision] controls whether the size of the
         * loaded image must match the request's size exactly or not.
         *
         * Default: [Precision.AUTOMATIC]
         */
        fun precision(precision: Precision) = apply {
            this.defaults = this.defaults.copy(precision = precision)
        }

        /**
         * Set the preferred [Bitmap.Config].
         *
         * This is not guaranteed and a different config may be used in some situations.
         *
         * Default: [DEFAULT_BITMAP_CONFIG]
         */
        fun bitmapConfig(bitmapConfig: Bitmap.Config) = apply {
            this.defaults = this.defaults.copy(bitmapConfig = bitmapConfig)
        }

        /**
         * A convenience function to set [fetcherDispatcher], [decoderDispatcher], and
         * [transformationDispatcher] in one call.
         *
         * Default: [Dispatchers.IO], [Dispatchers.Default], [Dispatchers.Default]
         */
        fun dispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.defaults = this.defaults.copy(
                fetcherDispatcher = dispatcher,
                decoderDispatcher = dispatcher,
                transformationDispatcher = dispatcher
            )
        }

        /**
         * The [CoroutineDispatcher] that the [Interceptor] chain will be executed on.
         *
         * Default: `Dispatchers.Main.immediate`
         */
        fun interceptorDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.defaults = this.defaults.copy(interceptorDispatcher = dispatcher)
        }

        /**
         * The [CoroutineDispatcher] that [Fetcher.fetch] will be executed on.
         *
         * Default: [Dispatchers.IO]
         */
        fun fetcherDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.defaults = this.defaults.copy(fetcherDispatcher = dispatcher)
        }

        /**
         * The [CoroutineDispatcher] that [Decoder.decode] will be executed on.
         *
         * Default: [Dispatchers.IO]
         */
        fun decoderDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.defaults = this.defaults.copy(decoderDispatcher = dispatcher)
        }

        /**
         * The [CoroutineDispatcher] that [Transformation.transform] will be executed on.
         *
         * Default: [Dispatchers.IO]
         */
        fun transformationDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.defaults = this.defaults.copy(transformationDispatcher = dispatcher)
        }

        /**
         * Set the default placeholder drawable to use when a request starts.
         */
        fun placeholder(@DrawableRes drawableResId: Int) =
            placeholder(applicationContext.getDrawableCompat(drawableResId))

        /**
         * Set the default placeholder drawable to use when a request starts.
         */
        fun placeholder(drawable: Drawable?) = apply {
            this.defaults = this.defaults.copy(placeholder = drawable)
        }

        /**
         * Set the default error drawable to use when a request fails.
         */
        fun error(@DrawableRes drawableResId: Int) =
            error(applicationContext.getDrawableCompat(drawableResId))

        /**
         * Set the default error drawable to use when a request fails.
         */
        fun error(drawable: Drawable?) = apply {
            this.defaults = this.defaults.copy(error = drawable)
        }

        /**
         * Set the default fallback drawable to use if [ImageRequest.data] is null.
         */
        fun fallback(@DrawableRes drawableResId: Int) =
            fallback(applicationContext.getDrawableCompat(drawableResId))

        /**
         * Set the default fallback drawable to use if [ImageRequest.data] is null.
         */
        fun fallback(drawable: Drawable?) = apply {
            this.defaults = this.defaults.copy(fallback = drawable)
        }

        /**
         * Set the default memory cache policy.
         */
        fun memoryCachePolicy(policy: CachePolicy) = apply {
            this.defaults = this.defaults.copy(memoryCachePolicy = policy)
        }

        /**
         * Set the default disk cache policy.
         */
        fun diskCachePolicy(policy: CachePolicy) = apply {
            this.defaults = this.defaults.copy(diskCachePolicy = policy)
        }

        /**
         * Set the default network cache policy.
         *
         * NOTE: Disabling writes has no effect.
         */
        fun networkCachePolicy(policy: CachePolicy) = apply {
            this.defaults = this.defaults.copy(networkCachePolicy = policy)
        }

        /**
         * Set the [Logger] to write logs to.
         *
         * NOTE: Setting a [Logger] can reduce performance and should be avoided in release builds.
         */
        fun logger(logger: Logger?) = apply {
            this.logger = logger
        }

        /**
         * Create a new [ImageLoader] instance.
         */
        fun build(): ImageLoader {
            return RealImageLoader(
                context = applicationContext,
                defaults = defaults,
                memoryCache = (memoryCache ?: Option(MemoryCache.Builder(applicationContext).build())).value,
                diskCacheLazy = (diskCache ?: Option(lazy { Utils.singletonDiskCache(applicationContext) })).value,
                callFactory = callFactory ?: lazyCallFactory { OkHttpClient() },
                eventListenerFactory = eventListenerFactory ?: EventListener.Factory.NONE,
                componentRegistry = componentRegistry ?: ComponentRegistry(),
                options = options,
                logger = logger
            )
        }

        @Deprecated(
            message = "Migrate to 'memoryCache'.",
            replaceWith = ReplaceWith(
                expression = "memoryCache(MemoryCache.Builder(context).maxSizePercent(percent).build())",
                imports = ["coil.memory.MemoryCache"]
            ),
            level = DeprecationLevel.ERROR // Temporary migration aid.
        )
        fun availableMemoryPercentage(@FloatRange(from = 0.0, to = 1.0) percent: Double): Builder = unsupported()

        @Deprecated(
            message = "Migrate to 'memoryCache'.",
            replaceWith = ReplaceWith(
                expression = "memoryCache(MemoryCache.Builder(context).weakReferencesEnabled(percent).build())",
                imports = ["coil.memory.MemoryCache"]
            ),
            level = DeprecationLevel.ERROR // Temporary migration aid.
        )
        fun trackWeakReferences(enable: Boolean): Builder = unsupported()

        @Deprecated(
            message = "Migrate to 'interceptorDispatcher'.",
            replaceWith = ReplaceWith(
                expression = "interceptorDispatcher(if (enable) Dispatchers.Main.immediate else Dispatchers.IO)",
                imports = ["kotlinx.coroutines.Dispatchers"]
            ),
            level = DeprecationLevel.ERROR // Temporary migration aid.
        )
        fun launchInterceptorChainOnMainThread(enable: Boolean): Builder = unsupported()

        @Deprecated(
            message = "Replace with 'components'.",
            replaceWith = ReplaceWith("components(builder)"),
            level = DeprecationLevel.ERROR // Temporary migration aid.
        )
        @JvmSynthetic
        fun componentRegistry(builder: ComponentRegistry.Builder.() -> Unit): Builder = unsupported()

        @Deprecated(
            message = "Replace with 'components'.",
            replaceWith = ReplaceWith("components(registry)"),
            level = DeprecationLevel.ERROR // Temporary migration aid.
        )
        fun componentRegistry(registry: ComponentRegistry): Builder = unsupported()

        @Deprecated(
            message = "Migrate to 'transitionFactory'.",
            replaceWith = ReplaceWith("transitionFactory { _, _ -> transition }"),
            level = DeprecationLevel.ERROR // Temporary migration aid.
        )
        fun transition(transition: Transition): Builder = unsupported()
    }

    companion object {
        /** Create a new [ImageLoader] without configuration. */
        @JvmStatic
        @JvmName("create")
        operator fun invoke(context: Context) = Builder(context).build()
    }
}
