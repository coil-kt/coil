package coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleObserver
import coil.annotation.ExperimentalCoilApi
import coil.bitmappool.BitmapPool
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
import coil.fetch.HttpUriFetcher
import coil.fetch.HttpUrlFetcher
import coil.fetch.ResourceUriFetcher
import coil.fetch.SourceResult
import coil.map.FileUriMapper
import coil.map.Mapper
import coil.map.MeasuredMapper
import coil.map.ResourceIntMapper
import coil.map.ResourceUriMapper
import coil.map.StringMapper
import coil.memory.BitmapReferenceCounter
import coil.memory.DelegateService
import coil.memory.MemoryCache
import coil.memory.RequestService
import coil.memory.TargetDelegate
import coil.memory.WeakMemoryCache
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
import coil.target.Target
import coil.target.ViewTarget
import coil.transform.Transformation
import coil.util.ComponentCallbacks
import coil.util.Emoji
import coil.util.Logger
import coil.util.bitmapConfigOrDefault
import coil.util.closeQuietly
import coil.util.emoji
import coil.util.errorOrDefault
import coil.util.fallbackOrDefault
import coil.util.firstNotNullIndices
import coil.util.forEachIndices
import coil.util.getValue
import coil.util.log
import coil.util.placeholderOrDefault
import coil.util.putValue
import coil.util.requestManager
import coil.util.safeConfig
import coil.util.takeIf
import coil.util.toDrawable
import coil.util.toSoftware
import coil.util.validateFetcher
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

@OptIn(ExperimentalCoilApi::class)
internal class RealImageLoader(
    private val context: Context,
    override val defaults: DefaultRequestOptions,
    private val bitmapPool: BitmapPool,
    private val referenceCounter: BitmapReferenceCounter,
    private val memoryCache: MemoryCache,
    private val weakMemoryCache: WeakMemoryCache,
    callFactory: Call.Factory,
    private val eventListenerFactory: EventListener.Factory,
    registry: ComponentRegistry,
    private val logger: Logger?
) : ImageLoader, ComponentCallbacks {

    companion object {
        private const val TAG = "RealImageLoader"
    }

    private val loaderScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable -> logger?.log(TAG, throwable) }

    private val delegateService = DelegateService(this, referenceCounter, logger)
    private val requestService = RequestService(defaults, logger)
    private val drawableDecoder = DrawableDecoderService(bitmapPool)
    private val networkObserver = NetworkObserver(context, logger)

    private val registry = registry.newBuilder()
        // Mappers
        .add(StringMapper())
        .add(FileUriMapper())
        .add(ResourceUriMapper(context))
        .add(ResourceIntMapper(context))
        // Fetchers
        .add(HttpUriFetcher(callFactory))
        .add(HttpUrlFetcher(callFactory))
        .add(FileFetcher())
        .add(AssetUriFetcher(context))
        .add(ContentUriFetcher(context))
        .add(ResourceUriFetcher(context, drawableDecoder))
        .add(DrawableFetcher(context, drawableDecoder))
        .add(BitmapFetcher(context))
        // Decoders
        .add(BitmapFactoryDecoder(context))
        .build()

    // isShutdown is only accessed from the main thread.
    private var isShutdown = false

    init {
        context.registerComponentCallbacks(this)
    }

    override fun execute(request: LoadRequest): RequestDisposable {
        // Start loading the data.
        val job = loaderScope.launch(exceptionHandler) { executeRequest(request) }

        return if (request.target is ViewTarget<*>) {
            val requestId = request.target.view.requestManager.setCurrentRequestJob(job)
            ViewTargetRequestDisposable(requestId, request.target)
        } else {
            BaseTargetRequestDisposable(job)
        }
    }

    override suspend fun execute(request: GetRequest): Drawable = executeRequest(request)

    private suspend fun executeRequest(request: Request): Drawable = withContext(Dispatchers.Main.immediate) outerJob@{
        // Ensure this image loader isn't shutdown.
        check(!isShutdown) { "The image loader is shutdown." }

        // Create a new event listener.
        val eventListener = eventListenerFactory.newListener(request)

        // Compute lifecycle info on the main thread.
        val (lifecycle, mainDispatcher) = requestService.lifecycleInfo(request)

        // Wrap the target to support bitmap pooling.
        val targetDelegate = delegateService.createTargetDelegate(request, eventListener)

        val deferred = async<Drawable>(mainDispatcher, CoroutineStart.LAZY) innerJob@{
            // Fail before starting if data is null.
            val data = request.data ?: throw NullRequestDataException()

            // Notify the listener that the request has started.
            eventListener.onStart(request)
            request.listener?.onStart(request)

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

            // Prepare to resolve the size lazily.
            val sizeResolver = requestService.sizeResolver(request, context)
            val lazySizeResolver = LazySizeResolver(this, sizeResolver, targetDelegate, request, defaults, eventListener)

            // Perform any data mapping.
            eventListener.mapStart(request)
            val mappedData = mapData(data, lazySizeResolver)
            eventListener.mapEnd(request, mappedData)

            // Compute the cache key.
            val fetcher = request.validateFetcher(mappedData) ?: registry.requireFetcher(mappedData)
            val cacheKey = request.key ?: computeCacheKey(fetcher, mappedData, request.parameters, request.transformations, lazySizeResolver)

            // Check the memory cache.
            val memoryCachePolicy = request.memoryCachePolicy ?: defaults.memoryCachePolicy
            val cachedValue = takeIf(memoryCachePolicy.readEnabled) {
                memoryCache.getValue(cacheKey) ?: request.aliasKeys.firstNotNullIndices { memoryCache.getValue(it) }
            }

            // Ignore the cached bitmap if it is hardware-backed and the request disallows hardware bitmaps.
            val cachedDrawable = cachedValue?.bitmap
                ?.takeIf { requestService.isConfigValidForHardware(request, it.safeConfig) }
                ?.toDrawable(context)

            // If we didn't resolve the size earlier, resolve it now.
            val size = lazySizeResolver.size(cachedDrawable)

            // Resolve the scale.
            val scale = requestService.scale(request, sizeResolver)

            // Short circuit if the cached drawable is valid for the target.
            if (cachedDrawable != null && isCachedDrawableValid(cachedDrawable, cachedValue.isSampled, size, scale, request)) {
                logger?.log(TAG, Log.INFO) { "${Emoji.BRAIN} Cached - $data" }
                targetDelegate.success(cachedDrawable, true, request.transition ?: defaults.transition)
                eventListener.onSuccess(request, DataSource.MEMORY)
                request.listener?.onSuccess(request, DataSource.MEMORY)
                return@innerJob cachedDrawable
            }

            // Fetch and decode the image.
            val (drawable, isSampled, source) = loadData(data, request, fetcher, mappedData, size, scale, eventListener)

            // Cache the result.
            if (memoryCachePolicy.writeEnabled) {
                memoryCache.putValue(cacheKey, drawable, isSampled)
            }

            // Set the final result on the target.
            logger?.log(TAG, Log.INFO) { "${source.emoji} Successful (${source.name}) - $data" }
            targetDelegate.success(drawable, false, request.transition ?: defaults.transition)
            eventListener.onSuccess(request, source)
            request.listener?.onSuccess(request, source)

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
                    logger?.log(TAG, Log.INFO) { "${Emoji.CONSTRUCTION} Cancelled - ${request.data}" }
                    eventListener.onCancel(request)
                    request.listener?.onCancel(request)
                } else {
                    logger?.log(TAG, Log.INFO) { "${Emoji.SIREN} Failed - ${request.data} - $throwable" }
                    val drawable = if (throwable is NullRequestDataException) {
                        request.fallbackOrDefault(defaults)
                    } else {
                        request.errorOrDefault(defaults)
                    }
                    targetDelegate.error(drawable, request.transition ?: defaults.transition)
                    eventListener.onError(request, throwable)
                    request.listener?.onError(request, throwable)
                }
            }
        }

        // Suspend the outer job until the inner job completes.
        return@outerJob deferred.await()
    }

    /** Map [data] using the components registered in [registry]. */
    @Suppress("UNCHECKED_CAST")
    @VisibleForTesting
    internal suspend inline fun mapData(data: Any, lazySizeResolver: LazySizeResolver): Any {
        var mappedData = data
        registry.measuredMappers.forEachIndices { (type, mapper) ->
            if (type.isAssignableFrom(mappedData::class.java) && (mapper as MeasuredMapper<Any, *>).handles(mappedData)) {
                mappedData = mapper.map(mappedData, lazySizeResolver.size())
            }
        }
        registry.mappers.forEachIndices { (type, mapper) ->
            if (type.isAssignableFrom(mappedData::class.java) && (mapper as Mapper<Any, *>).handles(mappedData)) {
                mappedData = mapper.map(mappedData)
            }
        }
        return mappedData
    }

    /** Compute the cache key for the [data] + [parameters] + [transformations] + [lazySizeResolver]. */
    @VisibleForTesting
    internal suspend inline fun <T : Any> computeCacheKey(
        fetcher: Fetcher<T>,
        data: T,
        parameters: Parameters,
        transformations: List<Transformation>,
        lazySizeResolver: LazySizeResolver
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
                transformations.forEachIndices { append('#').append(it.key()) }

                // Append the size if there are any transformations.
                append('#').append(lazySizeResolver.size())
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
                    dstWidth = size.width,
                    dstHeight = size.height,
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
        if (!requestService.isConfigValidForHardware(request, bitmap.safeConfig)) {
            return false
        }

        // Allow returning a cached RGB_565 bitmap if allowRgb565 is enabled.
        if ((request.allowRgb565 ?: defaults.allowRgb565) && bitmap.config == Bitmap.Config.RGB_565) {
            return true
        }

        // The cached bitmap is valid if its config matches the requested config.
        return bitmap.config.toSoftware() == request.bitmapConfigOrDefault(defaults).toSoftware()
    }

    /** Load the [data] as a [Drawable]. Apply any [Transformation]s. */
    private suspend inline fun loadData(
        data: Any,
        request: Request,
        fetcher: Fetcher<Any>,
        mappedData: Any,
        size: Size,
        scale: Scale,
        eventListener: EventListener
    ): DrawableResult = withContext(request.dispatcher ?: defaults.dispatcher) {
        val options = requestService.options(request, size, scale, networkObserver.isOnline())

        eventListener.fetchStart(request, fetcher, options)
        val fetchResult = fetcher.fetch(bitmapPool, mappedData, size, options)
        eventListener.fetchEnd(request, fetcher, options)

        val baseResult = when (fetchResult) {
            is SourceResult -> {
                val decodeResult = try {
                    // Check if we're cancelled.
                    ensureActive()

                    // Find the relevant decoder.
                    val isDiskOnlyPreload = request is LoadRequest && request.target == null &&
                        !(request.memoryCachePolicy ?: defaults.memoryCachePolicy).writeEnabled
                    val decoder = if (isDiskOnlyPreload) {
                        // Skip decoding the result if we are preloading the data and writing to the memory cache is disabled.
                        // Instead, we exhaust the source and return an empty result.
                        EmptyDecoder
                    } else {
                        request.decoder ?: registry.requireDecoder(data, fetchResult.source, fetchResult.mimeType)
                    }

                    // Decode the stream.
                    eventListener.decodeStart(request, decoder, options)
                    val decodeResult = decoder.decode(bitmapPool, fetchResult.source, size, options)
                    eventListener.decodeEnd(request, decoder, options)
                    decodeResult
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
        val finalResult = applyTransformations(this, baseResult, request, size, options, eventListener)
        (finalResult.drawable as? BitmapDrawable)?.bitmap?.prepareToDraw()

        return@withContext finalResult
    }

    /** Apply any [Transformation]s and return an updated [DrawableResult]. */
    @VisibleForTesting
    internal suspend inline fun applyTransformations(
        scope: CoroutineScope,
        result: DrawableResult,
        request: Request,
        size: Size,
        options: Options,
        eventListener: EventListener
    ): DrawableResult = scope.run {
        val transformations = request.transformations
        if (transformations.isEmpty()) {
            return@run result
        }

        // Convert the drawable into a bitmap.
        eventListener.transformStart(request)
        val baseBitmap = if (result.drawable is BitmapDrawable) {
            result.drawable.bitmap
        } else {
            logger?.log(TAG, Log.INFO) {
                "Converting drawable of type ${result.drawable::class.java.canonicalName} " +
                    "to apply transformations: $transformations"
            }
            drawableDecoder.convert(result.drawable, size, options.config)
        }

        val transformedBitmap = transformations.fold(baseBitmap) { bitmap, transformation ->
            transformation.transform(bitmapPool, bitmap, size).also { ensureActive() }
        }
        val transformedResult = result.copy(drawable = transformedBitmap.toDrawable(context))
        eventListener.transformEnd(request)
        return@run transformedResult
    }

    override fun invalidate(key: String) {
        memoryCache.invalidate(key)
        weakMemoryCache.invalidate(key)
    }

    override fun onTrimMemory(level: Int) {
        memoryCache.trimMemory(level)
        weakMemoryCache.trimMemory(level)
        bitmapPool.trimMemory(level)
    }

    override fun clearMemory() {
        memoryCache.clearMemory()
        weakMemoryCache.clearMemory()
        bitmapPool.clear()
    }

    override fun shutdown() {
        if (isShutdown) return
        isShutdown = true

        loaderScope.cancel()
        context.unregisterComponentCallbacks(this)
        networkObserver.shutdown()
        clearMemory()
    }

    /** Lazily resolves and caches a request's size. Responsible for calling [Target.onStart]. */
    @VisibleForTesting
    internal class LazySizeResolver(
        private val scope: CoroutineScope,
        private val sizeResolver: SizeResolver,
        private val targetDelegate: TargetDelegate,
        private val request: Request,
        private val defaults: DefaultRequestOptions,
        private val eventListener: EventListener
    ) {

        private var size: Size? = null

        @MainThread
        suspend inline fun size(cached: BitmapDrawable? = null): Size = scope.run {
            size?.let { return@run it }

            // Call the target's onStart before resolving the size.
            targetDelegate.start(cached, cached ?: request.placeholderOrDefault(defaults))

            eventListener.resolveSizeStart(request)
            val size = sizeResolver.size().also { size = it }
            eventListener.resolveSizeEnd(request, size)

            ensureActive()
            return@run size
        }
    }
}
