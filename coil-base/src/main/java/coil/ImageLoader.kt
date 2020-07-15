@file:Suppress("NOTHING_TO_INLINE", "unused")
@file:OptIn(ExperimentalCoilApi::class)

package coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import coil.annotation.ExperimentalCoilApi
import coil.bitmap.BitmapPool
import coil.bitmap.BitmapReferenceCounter
import coil.bitmap.RealBitmapPool
import coil.drawable.CrossfadeDrawable
import coil.memory.EmptyWeakMemoryCache
import coil.memory.MemoryCache
import coil.memory.RealWeakMemoryCache
import coil.memory.StrongMemoryCache
import coil.request.CachePolicy
import coil.request.DefaultRequestOptions
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.RequestDisposable
import coil.request.RequestResult
import coil.request.SuccessResult
import coil.size.Precision
import coil.target.ViewTarget
import coil.transition.CrossfadeTransition
import coil.transition.Transition
import coil.util.CoilUtils
import coil.util.Logger
import coil.util.Utils
import coil.util.getDrawableCompat
import coil.util.lazyCallFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.Call
import okhttp3.OkHttpClient

/**
 * A service class that loads images by executing [ImageRequest]s. Image loaders handle caching, data fetching,
 * image decoding, request management, bitmap pooling, memory management, and more.
 *
 * Image loaders are designed to be shareable and work best when you create a single instance and
 * share it throughout your app.
 *
 * It's recommended, though not required, to call [shutdown] when you've finished using an image loader.
 * This preemptively frees its memory and cleans up any observers.
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
     * @return A [RequestDisposable] which can be used to cancel or check the status of the request.
     */
    fun enqueue(request: ImageRequest): RequestDisposable

    /**
     * Execute the [request] in the current coroutine scope.
     *
     * NOTE: If [ImageRequest.target] is a [ViewTarget], the job will automatically be cancelled
     * if its view is detached.
     *
     * @param request The request to execute.
     * @return A [SuccessResult] if the request completes successfully. Else, returns an [ErrorResult].
     */
    suspend fun execute(request: ImageRequest): RequestResult

    /**
     * Shutdown this image loader.
     *
     * All associated resources will be freed and any new requests will fail before starting.
     *
     * In progress [enqueue] requests will be cancelled immediately.
     * In progress [execute] requests will continue until complete.
     */
    fun shutdown()

    /**
     * Remove the value referenced by [key] from the memory cache.
     *
     * @param key The cache key to remove.
     */
    @Deprecated(
        message = "Call the memory cache directly.",
        replaceWith = ReplaceWith("memoryCache.remove(MemoryCache.Key(key))", "coil.memory.MemoryCache")
    )
    fun invalidate(key: String) {
        memoryCache.remove(MemoryCache.Key(key))
    }

    /**
     * Clear this image loader's memory cache and bitmap pool.
     */
    @Deprecated(
        message = "Call the memory cache and bitmap pool directly.",
        replaceWith = ReplaceWith("apply { memoryCache.clear(); bitmapPool.clear() }")
    )
    fun clearMemory() {
        memoryCache.clear()
        bitmapPool.clear()
    }

    class Builder(context: Context) {

        private val applicationContext = context.applicationContext

        private var callFactory: Call.Factory? = null
        private var eventListenerFactory: EventListener.Factory? = null
        private var registry: ComponentRegistry? = null
        private var logger: Logger? = null
        private var defaults = DefaultRequestOptions()

        private var availableMemoryPercentage = Utils.getDefaultAvailableMemoryPercentage(applicationContext)
        private var bitmapPoolPercentage = Utils.getDefaultBitmapPoolPercentage()
        private var trackWeakReferences = true

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
            this.registry = registry
        }

        /**
         * Set the percentage of available memory to devote to this [ImageLoader]'s memory cache and bitmap pool.
         *
         * Setting this to 0 disables memory caching and bitmap pooling.
         *
         * Default: [Utils.getDefaultAvailableMemoryPercentage]
         */
        fun availableMemoryPercentage(@FloatRange(from = 0.0, to = 1.0) multiplier: Double) = apply {
            require(multiplier in 0.0..1.0) { "Multiplier must be within the range [0.0, 1.0]." }
            this.availableMemoryPercentage = multiplier
        }

        /**
         * Set the percentage of memory allocated to this [ImageLoader] to allocate to bitmap pooling.
         *
         * i.e. Setting [availableMemoryPercentage] to 0.25 and [bitmapPoolPercentage] to 0.5 allows this ImageLoader
         * to use 25% of the app's total memory and splits that memory 50/50 between the bitmap pool and memory cache.
         *
         * Setting this to 0 disables bitmap pooling.
         *
         * Default: [Utils.getDefaultBitmapPoolPercentage]
         */
        fun bitmapPoolPercentage(@FloatRange(from = 0.0, to = 1.0) multiplier: Double) = apply {
            require(multiplier in 0.0..1.0) { "Multiplier must be within the range [0.0, 1.0]." }
            this.bitmapPoolPercentage = multiplier
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
         * Enables weak reference tracking of loaded images.
         *
         * This allows the image loader to hold weak references to loaded images.
         * This ensures that if an image is still in memory it will be returned from the memory cache.
         *
         * Default: true
         */
        fun trackWeakReferences(enable: Boolean) = apply {
            this.trackWeakReferences = enable
        }

        /**
         * Set a single [EventListener] that will receive all callbacks for requests launched by this image loader.
         */
        @ExperimentalCoilApi
        fun eventListener(listener: EventListener) = eventListener(EventListener.Factory(listener))

        /**
         * Set the [EventListener.Factory] to create per-request [EventListener]s.
         *
         * @see eventListener
         */
        @ExperimentalCoilApi
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
            val availableMemorySize = Utils.calculateAvailableMemorySize(applicationContext, availableMemoryPercentage)
            val bitmapPoolSize = (bitmapPoolPercentage * availableMemorySize).toInt()
            val memoryCacheSize = (availableMemorySize - bitmapPoolSize).toInt()

            val bitmapPool = RealBitmapPool(bitmapPoolSize, logger = logger)
            val weakMemoryCache = if (trackWeakReferences) RealWeakMemoryCache(logger) else EmptyWeakMemoryCache
            val referenceCounter =
                BitmapReferenceCounter(weakMemoryCache, bitmapPool, logger)
            val memoryCache = StrongMemoryCache(weakMemoryCache, referenceCounter, memoryCacheSize, logger)

            return RealImageLoader(
                context = applicationContext,
                defaults = defaults,
                bitmapPool = bitmapPool,
                referenceCounter = referenceCounter,
                strongMemoryCache = memoryCache,
                weakMemoryCache = weakMemoryCache,
                callFactory = callFactory ?: buildDefaultCallFactory(),
                eventListenerFactory = eventListenerFactory ?: EventListener.Factory.NONE,
                componentRegistry = registry ?: ComponentRegistry(),
                logger = logger
            )
        }

        private fun buildDefaultCallFactory() = lazyCallFactory {
            OkHttpClient.Builder()
                .cache(CoilUtils.createDefaultCache(applicationContext))
                .build()
        }
    }

    companion object {
        /** Create a new [ImageLoader] without configuration. */
        @JvmStatic
        @JvmName("create")
        inline operator fun invoke(context: Context) = Builder(context).build()
    }
}
