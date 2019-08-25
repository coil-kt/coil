@file:Suppress("unused")

package coil

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleObserver
import coil.bitmappool.RealBitmapPool
import coil.decode.BitmapFactoryDecoder
import coil.decode.DataSource
import coil.decode.DrawableDecoderService
import coil.decode.EmptyDecoder
import coil.fetch.BitmapFetcher
import coil.fetch.DrawableFetcher
import coil.fetch.DrawableResult
import coil.fetch.Fetcher
import coil.fetch.HttpUrlFetcher
import coil.fetch.ResourceFetcher
import coil.fetch.SourceResult
import coil.fetch.UriFetcher
import coil.map.FileMapper
import coil.map.HttpUriMapper
import coil.map.StringMapper
import coil.memory.BitmapReferenceCounter
import coil.memory.DelegateService
import coil.memory.MemoryCache
import coil.memory.RequestService
import coil.network.NetworkObserver
import coil.request.BaseTargetRequestDisposable
import coil.request.EmptyRequestDisposable
import coil.request.GetRequest
import coil.request.LoadRequest
import coil.request.Request
import coil.request.RequestDisposable
import coil.request.ViewTargetRequestDisposable
import coil.size.PixelSize
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import coil.target.ViewTarget
import coil.transform.Transformation
import coil.util.ComponentCallbacks
import coil.util.Emoji
import coil.util.cancel
import coil.util.closeQuietly
import coil.util.emoji
import coil.util.getValue
import coil.util.log
import coil.util.normalize
import coil.util.putValue
import coil.util.takeIf
import coil.util.toDrawable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call

internal class RealImageLoader(
    private val context: Context,
    override val defaults: DefaultRequestOptions,
    bitmapPoolSize: Long,
    memoryCacheSize: Int,
    callFactory: Call.Factory,
    registry: ComponentRegistry
) : ImageLoader, ComponentCallbacks {

    companion object {
        private const val TAG = "RealImageLoader"
    }

    private val loaderScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable -> log(TAG, throwable) }

    private val bitmapPool = RealBitmapPool(bitmapPoolSize)
    private val referenceCounter = BitmapReferenceCounter(bitmapPool)
    private val delegateService = DelegateService(this, referenceCounter)
    private val requestService = RequestService()
    private val drawableDecoder = DrawableDecoderService(context, bitmapPool)
    private val memoryCache = MemoryCache(referenceCounter, memoryCacheSize)
    private val networkObserver = NetworkObserver(context)

    private val registry = ComponentRegistry(registry) {
        add(StringMapper())
        add(HttpUriMapper())
        add(FileMapper())

        add(HttpUrlFetcher(callFactory))
        add(UriFetcher(context))
        add(ResourceFetcher(context, drawableDecoder))
        add(DrawableFetcher(drawableDecoder))
        add(BitmapFetcher(context))

        add(BitmapFactoryDecoder(context))
    }

    private var isShutdown = false

    init {
        context.registerComponentCallbacks(this)
    }

    override fun load(request: LoadRequest): RequestDisposable {
        // Short circuit and cancel any attached requests if data is null.
        val data = request.data
        val target = request.target
        if (data == null) {
            if (target is ViewTarget<*>) {
                target.cancel()
            }
            return EmptyRequestDisposable
        }

        // Start loading the data.
        val job = loaderScope.launch(exceptionHandler) {
            execute(data, request)
        }

        return if (target is ViewTarget<*>) {
            ViewTargetRequestDisposable(target, request)
        } else {
            BaseTargetRequestDisposable(job)
        }
    }

    override suspend fun get(request: GetRequest): Drawable = execute(request.data, request)

    private suspend fun execute(
        data: Any,
        request: Request
    ): Drawable = withContext(Dispatchers.Main.immediate) outerJob@{
        // Ensure this image loader isn't shutdown.
        assertNotShutdown()

        // Compute lifecycle info on the main thread.
        val (lifecycle, mainDispatcher) = requestService.lifecycleInfo(request)

        // Wrap the target to support Bitmap pooling.
        val targetDelegate = delegateService.createTargetDelegate(request)

        val deferred = async<Drawable>(mainDispatcher, CoroutineStart.LAZY) innerJob@{
            request.listener?.onStart(data)

            // Add the target as a lifecycle observer, if necessary.
            val target = request.target
            if (target is ViewTarget<*> && target is LifecycleObserver) {
                lifecycle.addObserver(target)
            }

            // Invalidate the Bitmap if it was provided as input.
            when (data) {
                is BitmapDrawable -> referenceCounter.invalidate(data.bitmap)
                is Bitmap -> referenceCounter.invalidate(data)
            }

            var sizeResolver: SizeResolver? = null
            var size: Size? = null

            // Perform any data conversions and resolve the size early, if necessary.
            val measuredMapper = registry.getMeasuredMapper(data)
            val mappedData = if (measuredMapper != null) {
                targetDelegate.start(null, request.placeholder)
                sizeResolver = requestService.sizeResolver(request, context)
                size = sizeResolver.size().also { ensureActive() }

                measuredMapper.map(data, size)
            } else {
                registry.getMapper(data)?.map(data) ?: data
            }

            // Compute the cache key.
            val fetcher = registry.requireFetcher(mappedData)
            val cacheKey = request.keyOverride ?: computeCacheKey(fetcher, mappedData, request.transformations)

            // Check the memory cache and set the placeholder.
            val cachedValue = takeIf(request.memoryCachePolicy.readEnabled) {
                memoryCache.getValue(cacheKey)
            }
            val cachedDrawable = cachedValue?.bitmap?.toDrawable(context)

            // If we didn't resolve the size earlier, resolve it now.
            if (sizeResolver == null || size == null) {
                targetDelegate.start(cachedDrawable, cachedDrawable ?: request.placeholder)
                sizeResolver = requestService.sizeResolver(request, context)
                size = sizeResolver.size().also { ensureActive() }
            }

            // Short circuit if the cached drawable is valid for the target.
            if (cachedDrawable != null && isCachedDrawableValid(cachedDrawable, cachedValue.isSampled, size, request)) {
                log(TAG, Log.INFO) { "${Emoji.BRAIN} Cached - $data" }
                targetDelegate.success(cachedDrawable, 0)
                request.listener?.onSuccess(data, DataSource.MEMORY)
                return@innerJob cachedDrawable
            }

            // Resolve the scale.
            val scale = requestService.scale(request, sizeResolver)

            // Load the image.
            val (drawable, isSampled, source) = loadData(data, request, fetcher, mappedData, size, scale)

            // Cache the result.
            if (request.memoryCachePolicy.writeEnabled) {
                memoryCache.putValue(cacheKey, drawable, isSampled)
            }

            // Set the final result on the target.
            log(TAG, Log.INFO) { "${source.emoji} Successful (${source.name}) - $data" }
            targetDelegate.success(drawable, request.crossfadeMillis)
            request.listener?.onSuccess(data, source)

            return@innerJob drawable
        }

        // Wrap the request to manage its lifecycle.
        val requestDelegate = delegateService.createRequestDelegate(request, targetDelegate, lifecycle, mainDispatcher, deferred)

        deferred.invokeOnCompletion { throwable ->
            // Ensure callbacks are executed on the main thread.
            loaderScope.launch(Dispatchers.Main.immediate) {
                requestDelegate.onComplete()
                throwable ?: return@launch

                if (throwable is CancellationException) {
                    log(TAG, Log.INFO) { "${Emoji.CONSTRUCTION} Cancelled - $data" }
                    request.listener?.onCancel(data)
                } else {
                    log(TAG, Log.INFO) { "${Emoji.SIREN} Failed - $data - $throwable" }
                    targetDelegate.error(request.error, request.crossfadeMillis)
                    request.listener?.onError(data, throwable)
                }
            }
        }

        // Suspend the outer job until the inner job completes.
        return@outerJob deferred.await()
    }

    /**
     * Compute the cache key for the [data] + [transformations].
     */
    private fun <T : Any> computeCacheKey(
        fetcher: Fetcher<T>,
        data: T,
        transformations: List<Transformation>
    ): String? {
        val baseCacheKey = fetcher.key(data) ?: return null
        return buildString {
            append(baseCacheKey)
            transformations.forEach { append(it.key()) }
        }
    }

    /**
     * Return true if the [Bitmap] returned from [MemoryCache] satisfies the [Request].
     */
    @VisibleForTesting
    internal fun isCachedDrawableValid(
        cached: BitmapDrawable,
        isSampled: Boolean,
        size: Size,
        request: Request
    ): Boolean {
        if (size !is PixelSize) {
            return !isSampled
        }

        if (isSampled && (cached.bitmap.width < size.width || cached.bitmap.height < size.height)) {
            return false
        }
        if (SDK_INT >= O && !request.allowHardware && cached.bitmap.config == Bitmap.Config.HARDWARE) {
            return false
        }

        val cachedConfig = cached.bitmap.config.normalize()
        val requestedConfig = request.bitmapConfig.normalize()
        if (cachedConfig >= requestedConfig) {
            return true
        }
        if (request.allowRgb565 && cachedConfig == Bitmap.Config.RGB_565 && requestedConfig == Bitmap.Config.ARGB_8888) {
            return true
        }

        return false
    }

    /**
     * Load the [data] as a [Drawable]. Apply any [Transformation]s.
     */
    private suspend inline fun loadData(
        data: Any,
        request: Request,
        fetcher: Fetcher<Any>,
        mappedData: Any,
        size: Size,
        scale: Scale
    ): DrawableResult = withContext(request.dispatcher) {
        // Convert the data into a Drawable.
        val options = requestService.options(request, scale, networkObserver.isOnline())
        val result = when (val fetchResult = fetcher.fetch(bitmapPool, mappedData, size, options)) {
            is SourceResult -> {
                val decodeResult = try {
                    // Check if we're cancelled.
                    ensureActive()

                    // Find the relevant decoder.
                    val decoder = if (request.isDiskPreload()) {
                        // Skip decoding the result if we are preloading the data and writing to the memory cache is disabled.
                        // Instead, we exhaust the source and return an empty result.
                        EmptyDecoder
                    } else {
                        registry.requireDecoder(data, fetchResult.source, fetchResult.mimeType)
                    }

                    // Decode the stream.
                    decoder.decode(bitmapPool, fetchResult.source, size, options)
                } catch (rethrown: Exception) {
                    // NOTE: We only close the stream automatically if there is an uncaught exception.
                    // This allows custom decoders to continue to read the source after returning a Drawable.
                    fetchResult.source.closeQuietly()
                    throw rethrown
                }

                // Combine the fetch and decode operations' results.
                DrawableResult(
                    drawable = decodeResult.drawable,
                    isSampled = decodeResult.isSampled,
                    dataSource = fetchResult.dataSource
                )
            }
            is DrawableResult -> fetchResult
        }

        // Check if we're cancelled.
        ensureActive()

        // Transformations can only be applied to BitmapDrawables.
        val transformedResult = if (result.drawable is BitmapDrawable && request.transformations.isNotEmpty()) {
            val bitmap = request.transformations.fold(result.drawable.bitmap) { bitmap, transformation ->
                transformation.transform(bitmapPool, bitmap).also { ensureActive() }
            }
            result.copy(drawable = bitmap.toDrawable(context))
        } else {
            result
        }

        (transformedResult.drawable as? BitmapDrawable)?.bitmap?.prepareToDraw()
        return@withContext transformedResult
    }

    override fun onTrimMemory(level: Int) {
        memoryCache.trimMemory(level)
        bitmapPool.trimMemory(level)
    }

    override fun clearMemory() = onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)

    @Synchronized
    override fun shutdown() {
        if (isShutdown) {
            return
        }
        isShutdown = true

        loaderScope.cancel()
        context.unregisterComponentCallbacks(this)
        networkObserver.shutdown()
        clearMemory()
    }

    private fun Request.isDiskPreload(): Boolean {
        return this is LoadRequest && target == null && !memoryCachePolicy.writeEnabled
    }

    private fun assertNotShutdown() {
        check(!isShutdown) { "The image loader is shutdown!" }
    }
}
