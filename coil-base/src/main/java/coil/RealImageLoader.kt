@file:Suppress("unused")
@file:UseExperimental(ExperimentalCoil::class)

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
import coil.annotation.ExperimentalCoil
import coil.bitmappool.RealBitmapPool
import coil.decode.BitmapFactoryDecoder
import coil.decode.DataSource
import coil.decode.DecodeUtils
import coil.decode.DrawableDecoderService
import coil.decode.EmptyDecoder
import coil.decode.Options
import coil.extension.isNotEmpty
import coil.fetch.AssetUriFetcher
import coil.fetch.BitmapFetcher
import coil.fetch.ContentUriFetcher
import coil.fetch.DrawableFetcher
import coil.fetch.DrawableResult
import coil.fetch.Fetcher
import coil.fetch.FileFetcher
import coil.fetch.HttpUrlFetcher
import coil.fetch.ResourceUriFetcher
import coil.fetch.SourceResult
import coil.map.FileUriMapper
import coil.map.HttpUriMapper
import coil.map.Mapper
import coil.map.MeasuredMapper
import coil.map.ResourceIntMapper
import coil.map.ResourceUriMapper
import coil.map.StringMapper
import coil.memory.BitmapReferenceCounter
import coil.memory.DelegateService
import coil.memory.MemoryCache
import coil.memory.RequestService
import coil.network.NetworkObserver
import coil.request.BaseTargetRequestDisposable
import coil.request.GetRequest
import coil.request.LoadRequest
import coil.request.NullRequestDataException
import coil.request.Parameters
import coil.request.Request
import coil.request.RequestDisposable
import coil.request.ViewTargetRequestDisposable
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import coil.target.ViewTarget
import coil.transform.Transformation
import coil.transition.Transition
import coil.util.ComponentCallbacks
import coil.util.Emoji
import coil.util.closeQuietly
import coil.util.emoji
import coil.util.firstNotNull
import coil.util.getValue
import coil.util.isDiskPreload
import coil.util.log
import coil.util.normalize
import coil.util.putValue
import coil.util.requestManager
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

    private val registry = registry.newBuilder()
        // Mappers
        .add(StringMapper())
        .add(HttpUriMapper())
        .add(FileUriMapper())
        .add(ResourceUriMapper(context))
        .add(ResourceIntMapper(context))
        // Fetchers
        .add(HttpUrlFetcher(callFactory))
        .add(FileFetcher())
        .add(AssetUriFetcher(context))
        .add(ContentUriFetcher(context))
        .add(ResourceUriFetcher(context, drawableDecoder))
        .add(DrawableFetcher(drawableDecoder))
        .add(BitmapFetcher(context))
        // Decoders
        .add(BitmapFactoryDecoder(context))
        .build()

    private var isShutdown = false

    init {
        context.registerComponentCallbacks(this)
    }

    override fun load(request: LoadRequest): RequestDisposable {
        // Start loading the data.
        val job = loaderScope.launch(exceptionHandler) {
            execute(request.data, request)
        }

        return if (request.target is ViewTarget<*>) {
            val requestId = request.target.view.requestManager.setCurrentRequestJob(job)
            ViewTargetRequestDisposable(requestId, request.target)
        } else {
            BaseTargetRequestDisposable(job)
        }
    }

    override suspend fun get(request: GetRequest): Drawable = execute(request.data, request)

    @Suppress("UNCHECKED_CAST")
    private suspend fun execute(
        data: Any?,
        request: Request
    ): Drawable = withContext(Dispatchers.Main.immediate) outerJob@{
        // Ensure this image loader isn't shutdown.
        assertNotShutdown()

        // Compute lifecycle info on the main thread.
        val (lifecycle, mainDispatcher) = requestService.lifecycleInfo(request)

        // Wrap the target to support bitmap pooling.
        val targetDelegate = delegateService.createTargetDelegate(request)

        val deferred = async<Drawable>(mainDispatcher, CoroutineStart.LAZY) innerJob@{
            // Fail before starting if data is null.
            data ?: throw NullRequestDataException()

            // Notify the listener that the request has started.
            request.listener?.onStart(data)

            // Invalidate the bitmap if it was provided as input.
            when (data) {
                is BitmapDrawable -> referenceCounter.invalidate(data.bitmap)
                is Bitmap -> referenceCounter.invalidate(data)
            }

            // Add the target as a lifecycle observer, if necessary.
            val target = request.target
            if (target is ViewTarget<*> && target is LifecycleObserver) {
                lifecycle.addObserver(target)
            }

            // Perform any data conversions.
            var sizeResolver: SizeResolver? = null
            var size: Size? = null
            var mappedData: Any = data
            for ((type, mapper) in registry.measuredMappers) {
                if (type.isAssignableFrom(mappedData::class.java) && (mapper as MeasuredMapper<Any, *>).handles(mappedData)) {
                    if (sizeResolver == null || size == null) {
                        targetDelegate.start(null, request.placeholder)
                        sizeResolver = requestService.sizeResolver(request, context)
                        size = sizeResolver.size().also { ensureActive() }
                    }
                    mappedData = mapper.map(mappedData, size)
                }
            }
            for ((type, mapper) in registry.mappers) {
                if (type.isAssignableFrom(mappedData::class.java) && (mapper as Mapper<Any, *>).handles(mappedData)) {
                    mappedData = mapper.map(mappedData)
                }
            }

            // Compute the cache key.
            val fetcher = registry.requireFetcher(mappedData)
            val cacheKey = if (request.key != null) {
                request.key
            } else {
                if (request.transformations.isNotEmpty()) {
                    if (sizeResolver == null || size == null) {
                        targetDelegate.start(null, request.placeholder)
                        sizeResolver = requestService.sizeResolver(request, context)
                        size = sizeResolver.size().also { ensureActive() }
                    }
                }
                computeCacheKey(fetcher, mappedData, request.parameters, request.transformations, size)
            }

            // Check the memory cache.
            val cachedValue = takeIf(request.memoryCachePolicy.readEnabled) {
                memoryCache.getValue(cacheKey) ?: request.aliasKeys.firstNotNull { memoryCache.getValue(it) }
            }
            val cachedDrawable = cachedValue?.bitmap?.toDrawable(context)

            // If we didn't resolve the size earlier, resolve it now.
            if (sizeResolver == null || size == null) {
                targetDelegate.start(cachedDrawable, cachedDrawable ?: request.placeholder)
                sizeResolver = requestService.sizeResolver(request, context)
                size = sizeResolver.size().also { ensureActive() }
            }

            // Resolve the scale.
            val scale = requestService.scale(request, sizeResolver)

            // Short circuit if the cached drawable is valid for the target.
            if (cachedDrawable != null && isCachedDrawableValid(cachedDrawable, cachedValue.isSampled, size, scale, request)) {
                log(TAG, Log.INFO) { "${Emoji.BRAIN} Cached - $data" }
                val transition = request.transitionFactory?.newTransition(Transition.Event.CACHED)
                targetDelegate.success(cachedDrawable, transition)
                request.listener?.onSuccess(data, DataSource.MEMORY)
                return@innerJob cachedDrawable
            }

            // Fetch and decode the image.
            val (drawable, isSampled, source) = loadData(data, request, fetcher, mappedData, size, scale)

            // Cache the result.
            if (request.memoryCachePolicy.writeEnabled) {
                memoryCache.putValue(cacheKey, drawable, isSampled)
            }

            // Set the final result on the target.
            log(TAG, Log.INFO) { "${source.emoji} Successful (${source.name}) - $data" }
            val transition = request.transitionFactory?.newTransition(Transition.Event.SUCCESS)
            targetDelegate.success(drawable, transition)
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
                    val drawable = if (throwable is NullRequestDataException) request.fallback else request.error
                    val transition = request.transitionFactory?.newTransition(Transition.Event.ERROR)
                    targetDelegate.error(drawable, transition)
                    request.listener?.onError(data, throwable)
                }
            }
        }

        // Suspend the outer job until the inner job completes.
        return@outerJob deferred.await()
    }

    /** Compute the cache key for the [data] + [parameters] + [transformations] + [size]. */
    @VisibleForTesting
    internal fun <T : Any> computeCacheKey(
        fetcher: Fetcher<T>,
        data: T,
        parameters: Parameters,
        transformations: List<Transformation>,
        size: Size?
    ): String? {
        val baseCacheKey = fetcher.key(data) ?: return null

        return buildString {
            append(baseCacheKey)

            // Check isNotEmpty first to avoid allocating an Iterator.
            if (parameters.isNotEmpty()) {
                for ((key, entry) in parameters) {
                    val cacheKey = entry.cacheKey ?: continue
                    append('#').append(key).append('=').append(cacheKey)
                }
            }

            if (transformations.isNotEmpty()) {
                transformations.forEach { append('#').append(it.key()) }

                // Append the size if there are any transformations. Size must not be null here.
                append('#').append(checkNotNull(size))
            }
        }
    }

    /** Return true if the [Bitmap] returned from [MemoryCache] satisfies the [Request]. */
    @VisibleForTesting
    internal fun isCachedDrawableValid(
        cached: BitmapDrawable,
        isSampled: Boolean,
        size: Size,
        scale: Scale,
        request: Request
    ): Boolean {
        // Ensure the size is valid for the target.
        val bitmap = cached.bitmap
        when (size) {
            is OriginalSize -> {
                if (isSampled) {
                    return false
                }
            }
            is PixelSize -> {
                val multiple = DecodeUtils.computeSizeMultiplier(
                    srcWidth = bitmap.width,
                    srcHeight = bitmap.height,
                    destWidth = size.width,
                    destHeight = size.height,
                    scale = scale
                )
                if (multiple != 1.0 && !requestService.allowInexactSize(request)) {
                    return false
                }
                if (multiple > 1.0 && isSampled) {
                    return false
                }
            }
        }

        // Ensure we don't return a hardware bitmap if the request doesn't allow it.
        if (SDK_INT >= O && !request.allowHardware && bitmap.config == Bitmap.Config.HARDWARE) {
            return false
        }

        val cachedConfig = bitmap.config.normalize()
        val requestedConfig = request.bitmapConfig.normalize()

        // Allow returning a bitmap with an equal or greater config.
        if (cachedConfig >= requestedConfig) {
            return true
        }

        // Allow returning a lesser RGB_565 bitmap if enabled.
        if (request.allowRgb565 && cachedConfig == Bitmap.Config.RGB_565 && requestedConfig == Bitmap.Config.ARGB_8888) {
            return true
        }

        return false
    }

    /** Load the [data] as a [Drawable]. Apply any [Transformation]s. */
    private suspend inline fun loadData(
        data: Any,
        request: Request,
        fetcher: Fetcher<Any>,
        mappedData: Any,
        size: Size,
        scale: Scale
    ): DrawableResult = withContext(request.dispatcher) {
        // Convert the data into a Drawable.
        val options = requestService.options(request, size, scale, networkObserver.isOnline())
        val baseResult = when (val fetchResult = fetcher.fetch(bitmapPool, mappedData, size, options)) {
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
                        request.decoder ?: registry.requireDecoder(data, fetchResult.source, fetchResult.mimeType)
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

        // Apply any transformations and prepare to draw.
        val finalResult = applyTransformations(this, baseResult, request.transformations, size, options)
        (finalResult.drawable as? BitmapDrawable)?.bitmap?.prepareToDraw()

        return@withContext finalResult
    }

    /** Apply any [Transformation]s and return an updated [DrawableResult]. */
    @VisibleForTesting
    internal suspend inline fun applyTransformations(
        scope: CoroutineScope,
        result: DrawableResult,
        transformations: List<Transformation>,
        size: Size,
        options: Options
    ): DrawableResult = scope.run {
        if (transformations.isEmpty()) {
            return@run result
        }

        // Convert the drawable into a bitmap.
        val baseBitmap = if (result.drawable is BitmapDrawable) {
            result.drawable.bitmap
        } else {
            log(TAG, Log.INFO) {
                "Converting drawable of type ${result.drawable::class.java.canonicalName} " +
                    "to apply transformations: $transformations"
            }
            drawableDecoder.convert(result.drawable, size, options.config)
        }

        val transformedBitmap = transformations.fold(baseBitmap) { bitmap, transformation ->
            transformation.transform(bitmapPool, bitmap, size).also { ensureActive() }
        }
        return@run result.copy(drawable = transformedBitmap.toDrawable(context))
    }

    override fun onTrimMemory(level: Int) {
        memoryCache.trimMemory(level)
        bitmapPool.trimMemory(level)
    }

    override fun clearMemory() = onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)

    @Synchronized
    override fun shutdown() {
        if (isShutdown) return
        isShutdown = true

        loaderScope.cancel()
        context.unregisterComponentCallbacks(this)
        networkObserver.shutdown()
        clearMemory()
    }

    private fun assertNotShutdown() {
        check(!isShutdown) { "The image loader is shutdown!" }
    }
}
