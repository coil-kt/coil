@file:Suppress("NOTHING_TO_INLINE", "unused")

package coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import coil.annotation.ExperimentalCoilApi
import coil.bitmap.BitmapPool
import coil.bitmap.EmptyBitmapPool
import coil.bitmap.EmptyBitmapReferenceCounter
import coil.bitmap.RealBitmapPool
import coil.bitmap.RealBitmapReferenceCounter
import coil.drawable.CrossfadeDrawable
import coil.fetch.Fetcher
import coil.intercept.Interceptor
import coil.map.Mapper
import coil.memory.EmptyWeakMemoryCache
import coil.memory.MemoryCache
import coil.memory.RealMemoryCache
import coil.memory.RealWeakMemoryCache
import coil.memory.StrongMemoryCache
import coil.request.CachePolicy
import coil.request.DefaultRequestOptions
import coil.request.Disposable
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.size.Precision
import coil.target.PoolableViewTarget
import coil.target.ViewTarget
import coil.transition.CrossfadeTransition
import coil.transition.Transition
import coil.util.CoilUtils
import coil.util.ImageLoaderOptions
import coil.util.Logger
import coil.util.Utils
import coil.util.getDrawableCompat
import coil.util.lazyCallFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import java.io.File

/**
 * A service class that loads images by executing [ImageRequest]s. Image loaders handle caching, data fetching,
 * image decoding, request management, bitmap pooling, memory management, and more.
 *
 * Image loaders are designed to be shareable and work best when you create a single instance and
 * share it throughout your app.
 */
interface ImageLoader {

    /**
     * The default options that are used to fill in unset [ImageRequest] values.
     */
    val defaults: DefaultRequestOptions

    /**
     * An in-memory cache of recently loaded images.
     */
    val memoryCache: MemoryCache

    /**
     * An object pool of reusable [Bitmap]s.
     */
    val bitmapPool: BitmapPool

    /**
     * Enqueue the [request] to be executed asynchronously.
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
     * @return A [SuccessResult] if the request completes successfully. Else, returns an [ErrorResult].
     */
    suspend fun execute(request: ImageRequest): ImageResult

    /**
     * Shutdown this image loader.
     *
     * All associated resources will be freed and new requests will fail before starting.
     *
     * Shutting down an image loader is optional. It will be cleaned up automatically if dereferenced.
     *
     * In progress [enqueue] requests will be cancelled immediately.
     * In progress [execute] requests will continue until complete.
     */
    fun shutdown()

    /**
     * Create an [ImageLoader.Builder] that shares the same resources and configuration as this image loader.
     */
    fun newBuilder(): Builder

    class Builder {

        private val applicationContext: Context
        private var defaults: DefaultRequestOptions
        private var callFactory: Call.Factory?
        private var eventListenerFactory: EventListener.Factory?
        private var componentRegistry: ComponentRegistry?
        private var options: ImageLoaderOptions
        private var logger: Logger?
        private var memoryCache: RealMemoryCache?
        private var availableMemoryPercentage: Double
        private var bitmapPoolPercentage: Double
        private var bitmapPoolingEnabled: Boolean
        private var trackWeakReferences: Boolean

        constructor(context: Context) {
            applicationContext = context.applicationContext
            defaults = DefaultRequestOptions.INSTANCE
            callFactory = null
            eventListenerFactory = null
            componentRegistry = null
            options = ImageLoaderOptions()
            logger = null
            memoryCache = null
            availableMemoryPercentage = Utils.getDefaultAvailableMemoryPercentage(applicationContext)
            bitmapPoolPercentage = Utils.getDefaultBitmapPoolPercentage()
            bitmapPoolingEnabled = true
            trackWeakReferences = true
        }

        internal constructor(imageLoader: RealImageLoader) {
            applicationContext = imageLoader.context.applicationContext
            defaults = imageLoader.defaults
            callFactory = imageLoader.callFactory
            eventListenerFactory = imageLoader.eventListenerFactory
            componentRegistry = imageLoader.componentRegistry
            options = imageLoader.options
            logger = imageLoader.logger
            memoryCache = imageLoader.memoryCache
            availableMemoryPercentage = 0.0
            bitmapPoolPercentage = 0.0
            bitmapPoolingEnabled = true
            trackWeakReferences = true
        }

        /**
         * Set the [OkHttpClient] used for network requests.
         *
         * This is a convenience function for calling `callFactory(Call.Factory)`.
         *
         * NOTE: You must set [OkHttpClient.cache] to enable disk caching. A default
         * Coil disk cache instance can be created using [CoilUtils.createDefaultCache].
         */
        fun okHttpClient(okHttpClient: OkHttpClient) = callFactory(okHttpClient)

        /**
         * Set a lazy callback to create the [OkHttpClient] used for network requests.
         *
         * This is a convenience function for calling `callFactory(() -> Call.Factory)`.
         *
         * NOTE: You must set [OkHttpClient.cache] to enable disk caching. A default
         * Coil disk cache instance can be created using [CoilUtils.createDefaultCache].
         */
        fun okHttpClient(initializer: () -> OkHttpClient) = callFactory(initializer)

        /**
         * Set the [Call.Factory] used for network requests.
         *
         * Calling [okHttpClient] automatically sets this value.
         *
         * NOTE: You must set [OkHttpClient.cache] to enable disk caching. A default
         * Coil disk cache instance can be created using [CoilUtils.createDefaultCache].
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
         *
         * Calling [okHttpClient] automatically sets this value.
         *
         * NOTE: You must set [OkHttpClient.cache] to enable disk caching. A default
         * Coil disk cache instance can be created using [CoilUtils.createDefaultCache].
         */
        fun callFactory(initializer: () -> Call.Factory) = apply {
            this.callFactory = lazyCallFactory(initializer)
        }

        /**
         * Build and set the [ComponentRegistry].
         */
        @JvmSynthetic
        inline fun componentRegistry(
            builder: ComponentRegistry.Builder.() -> Unit
        ) = componentRegistry(ComponentRegistry.Builder().apply(builder).build())

        /**
         * Set the [ComponentRegistry].
         */
        fun componentRegistry(registry: ComponentRegistry) = apply {
            this.componentRegistry = registry
        }

        /**
         * Set the [MemoryCache]. This also sets the [BitmapPool] to the instance used by this [MemoryCache].
         *
         * This is useful for sharing [MemoryCache] and [BitmapPool] instances between [ImageLoader]s.
         *
         * NOTE: Custom memory cache implementations are currently not supported.
         */
        fun memoryCache(memoryCache: MemoryCache) = apply {
            require(memoryCache is RealMemoryCache) { "Custom memory cache implementations are currently not supported." }
            this.memoryCache = memoryCache
        }

        /**
         * Set the percentage of available memory to devote to this [ImageLoader]'s memory cache and bitmap pool.
         *
         * Setting this to 0 disables memory caching and bitmap pooling.
         *
         * Setting this value discards the shared memory cache set in [memoryCache].
         *
         * Default: [Utils.getDefaultAvailableMemoryPercentage]
         */
        fun availableMemoryPercentage(@FloatRange(from = 0.0, to = 1.0) percent: Double) = apply {
            require(percent in 0.0..1.0) { "Percent must be in the range [0.0, 1.0]." }
            this.availableMemoryPercentage = percent
            this.memoryCache = null
        }

        /**
         * Set the percentage of memory allocated to this [ImageLoader] to allocate to bitmap pooling.
         *
         * i.e. Setting [availableMemoryPercentage] to 0.25 and [bitmapPoolPercentage] to 0.5 allows this ImageLoader
         * to use 25% of the app's total memory and splits that memory 50/50 between the bitmap pool and memory cache.
         *
         * Setting this to 0 disables bitmap pooling.
         *
         * Setting this value discards the shared memory cache set in [memoryCache].
         *
         * Default: [Utils.getDefaultBitmapPoolPercentage]
         */
        fun bitmapPoolPercentage(@FloatRange(from = 0.0, to = 1.0) percent: Double) = apply {
            require(percent in 0.0..1.0) { "Percent must be in the range [0.0, 1.0]." }
            this.bitmapPoolPercentage = percent
            this.memoryCache = null
        }

        /**
         * The default [CoroutineDispatcher] to run image requests on.
         *
         * Default: [Dispatchers.IO]
         */
        fun dispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.defaults = this.defaults.copy(dispatcher = dispatcher)
        }

        /**
         * Allow the use of [Bitmap.Config.HARDWARE].
         *
         * If false, any use of [Bitmap.Config.HARDWARE] will be treated as [Bitmap.Config.ARGB_8888].
         *
         * NOTE: Setting this to false this will reduce performance on API 26 and above. Only disable if necessary.
         *
         * Default: true
         */
        fun allowHardware(enable: Boolean) = apply {
            this.defaults = this.defaults.copy(allowHardware = enable)
        }

        /**
         * Allow automatically using [Bitmap.Config.RGB_565] when an image is guaranteed to not have alpha.
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
         * Enables adding [File.lastModified] to the memory cache key when loading an image from a [File].
         *
         * This allows subsequent requests that load the same file to miss the memory cache if the file has been updated.
         * However, if the memory cache check occurs on the main thread (see [launchInterceptorChainOnMainThread])
         * calling [File.lastModified] will cause a strict mode violation.
         *
         * Default: true
         */
        fun addLastModifiedToFileCacheKey(enable: Boolean) = apply {
            this.options = this.options.copy(addLastModifiedToFileCacheKey = enable)
        }

        /**
         * Enables counting references to bitmaps so they can be automatically reused by a [BitmapPool]
         * when their reference count reaches zero.
         *
         * Only certain requests are eligible for bitmap pooling. See [PoolableViewTarget] for more information.
         *
         * If this is disabled, no bitmaps will be added to this [ImageLoader]'s [BitmapPool] automatically and
         * the [BitmapPool] will not be allocated any memory (this overrides [bitmapPoolPercentage]).
         *
         * Setting this value discards the shared memory cache set in [memoryCache].
         *
         * Default: true
         */
        fun bitmapPoolingEnabled(enable: Boolean) = apply {
            this.bitmapPoolingEnabled = enable
            this.memoryCache = null
        }

        /**
         * Enables weak reference tracking of loaded images.
         *
         * This allows the image loader to hold weak references to loaded images.
         * This ensures that if an image is still in memory it will be returned from the memory cache.
         *
         * Setting this value discards the shared memory cache set in [memoryCache].
         *
         * Default: true
         */
        fun trackWeakReferences(enable: Boolean) = apply {
            this.trackWeakReferences = enable
            this.memoryCache = null
        }

        /**
         * Enables launching the [Interceptor] chain on the main thread.
         *
         * If true, the [Interceptor] chain will be launched from [MainCoroutineDispatcher.immediate]. This allows
         * the [ImageLoader] to check its memory cache and return a cached value synchronously if the request is
         * started from the main thread. However, [Mapper.map] and [Fetcher.key] operations will be executed on the
         * main thread as well, which has a performance cost.
         *
         * If false, the [Interceptor] chain will be launched from the request's [ImageRequest.dispatcher].
         * This will result in better UI performance, but values from the memory cache will not be resolved
         * synchronously.
         *
         * The actual fetch + decode process always occurs on [ImageRequest.dispatcher] and is unaffected by this flag.
         *
         * It's worth noting that [Interceptor]s can also control which [CoroutineDispatcher] the
         * memory cache is checked on by calling [Interceptor.Chain.proceed] inside a [withContext] block.
         * Therefore if you set [launchInterceptorChainOnMainThread] to true, you can control which [ImageRequest]s
         * check the memory cache synchronously at runtime.
         *
         * Default: true
         */
        fun launchInterceptorChainOnMainThread(enable: Boolean) = apply {
            this.options = this.options.copy(launchInterceptorChainOnMainThread = enable)
        }

        /**
         * Set a single [EventListener] that will receive all callbacks for requests launched by this image loader.
         */
        fun eventListener(listener: EventListener) = eventListener(EventListener.Factory(listener))

        /**
         * Set the [EventListener.Factory] to create per-request [EventListener]s.
         *
         * @see eventListener
         */
        fun eventListener(factory: EventListener.Factory) = apply {
            this.eventListenerFactory = factory
        }

        /**
         * Enable a crossfade animation with duration [CrossfadeDrawable.DEFAULT_DURATION] milliseconds
         * when a request completes successfully.
         *
         * Default: false
         */
        fun crossfade(enable: Boolean) = crossfade(if (enable) CrossfadeDrawable.DEFAULT_DURATION else 0)

        /**
         * Enable a crossfade animation with [durationMillis] milliseconds when a request completes successfully.
         *
         * @see `crossfade(Boolean)`
         */
        fun crossfade(durationMillis: Int) =
            transition(if (durationMillis > 0) CrossfadeTransition(durationMillis) else Transition.NONE)

        /**
         * Set the default [Transition] for each request.
         */
        @ExperimentalCoilApi
        fun transition(transition: Transition) = apply {
            this.defaults = this.defaults.copy(transition = transition)
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
         * Default: [Utils.DEFAULT_BITMAP_CONFIG]
         */
        fun bitmapConfig(bitmapConfig: Bitmap.Config) = apply {
            this.defaults = this.defaults.copy(bitmapConfig = bitmapConfig)
        }

        /**
         * Set the default placeholder drawable to use when a request starts.
         */
        fun placeholder(@DrawableRes drawableResId: Int) = apply {
            this.defaults = this.defaults.copy(placeholder = applicationContext.getDrawableCompat(drawableResId))
        }

        /**
         * Set the default placeholder drawable to use when a request starts.
         */
        fun placeholder(drawable: Drawable?) = apply {
            this.defaults = this.defaults.copy(placeholder = drawable)
        }

        /**
         * Set the default error drawable to use when a request fails.
         */
        fun error(@DrawableRes drawableResId: Int) = apply {
            this.defaults = this.defaults.copy(error = applicationContext.getDrawableCompat(drawableResId))
        }

        /**
         * Set the default error drawable to use when a request fails.
         */
        fun error(drawable: Drawable?) = apply {
            this.defaults = this.defaults.copy(error = drawable)
        }

        /**
         * Set the default fallback drawable to use if [ImageRequest.data] is null.
         */
        fun fallback(@DrawableRes drawableResId: Int) = apply {
            this.defaults = this.defaults.copy(error = applicationContext.getDrawableCompat(drawableResId))
        }

        /**
         * Set the default fallback drawable to use if [ImageRequest.data] is null.
         */
        fun fallback(drawable: Drawable?) = apply {
            this.defaults = this.defaults.copy(error = drawable)
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
            val memoryCache = memoryCache ?: buildDefaultMemoryCache()
            return RealImageLoader(
                context = applicationContext,
                defaults = defaults,
                bitmapPool = memoryCache.bitmapPool,
                memoryCache = memoryCache,
                callFactory = callFactory ?: buildDefaultCallFactory(),
                eventListenerFactory = eventListenerFactory ?: EventListener.Factory.NONE,
                componentRegistry = componentRegistry ?: ComponentRegistry(),
                options = options,
                logger = logger
            )
        }

        private fun buildDefaultCallFactory() = lazyCallFactory {
            OkHttpClient.Builder()
                .cache(CoilUtils.createDefaultCache(applicationContext))
                .build()
        }

        private fun buildDefaultMemoryCache(): RealMemoryCache {
            val availableMemorySize = Utils.calculateAvailableMemorySize(applicationContext, availableMemoryPercentage)
            val bitmapPoolPercentage = if (bitmapPoolingEnabled) bitmapPoolPercentage else 0.0
            val bitmapPoolSize = (bitmapPoolPercentage * availableMemorySize).toInt()
            val memoryCacheSize = (availableMemorySize - bitmapPoolSize).toInt()

            val bitmapPool = if (bitmapPoolSize == 0) {
                EmptyBitmapPool()
            } else {
                RealBitmapPool(bitmapPoolSize, logger = logger)
            }
            val weakMemoryCache = if (trackWeakReferences) {
                RealWeakMemoryCache(logger)
            } else {
                EmptyWeakMemoryCache
            }
            val referenceCounter = if (bitmapPoolingEnabled) {
                RealBitmapReferenceCounter(weakMemoryCache, bitmapPool, logger)
            } else {
                EmptyBitmapReferenceCounter
            }
            val strongMemoryCache = StrongMemoryCache(weakMemoryCache, referenceCounter, memoryCacheSize, logger)
            return RealMemoryCache(strongMemoryCache, weakMemoryCache, referenceCounter, bitmapPool)
        }
    }

    companion object {
        /** Create a new [ImageLoader] without configuration. */
        @JvmStatic
        @JvmName("create")
        operator fun invoke(context: Context) = Builder(context).build()
    }
}
