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
import coil.decode.DrawableDecoderService
import coil.decode.EmptyDecoder
import coil.decode.Options
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
import coil.memory.MemoryCacheService
import coil.memory.RequestService
import coil.memory.TargetDelegate
import coil.memory.WeakMemoryCache
import coil.request.BaseTargetRequestDisposable
import coil.request.GetRequest
import coil.request.LoadRequest
import coil.request.NullRequestDataException
import coil.request.Parameters
import coil.request.Request
import coil.request.RequestDisposable
import coil.request.RequestResult
import coil.request.SuccessResult
import coil.request.ViewTargetRequestDisposable
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import coil.target.ViewTarget
import coil.transform.Transformation
import coil.util.Emoji
import coil.util.Logger
import coil.util.SystemCallbacks
import coil.util.awaitStarted
import coil.util.closeQuietly
import coil.util.emoji
import coil.util.firstNotNullIndices
import coil.util.foldIndices
import coil.util.forEachIndices
import coil.util.get
import coil.util.job
import coil.util.log
import coil.util.placeholderOrDefault
import coil.util.put
import coil.util.requestManager
import coil.util.safeConfig
import coil.util.takeIf
import coil.util.toDrawable
import coil.util.validateFetcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

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
    addLastModifiedToFileCacheKey: Boolean,
    val logger: Logger?
) : ImageLoader {

    private val loaderScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable -> logger?.log(TAG, throwable) }

    private val delegateService = DelegateService(this, referenceCounter, logger)
    private val requestService = RequestService(defaults, logger)
    private val memoryCacheService = MemoryCacheService(requestService, logger)
    private val drawableDecoder = DrawableDecoderService(bitmapPool)
    private val systemCallbacks = SystemCallbacks(this, context)

    private val registry = registry.newBuilder()
        // Mappers
        .add(StringMapper())
        .add(FileUriMapper())
        .add(ResourceUriMapper(context))
        .add(ResourceIntMapper(context))
        // Fetchers
        .add(HttpUriFetcher(callFactory))
        .add(HttpUrlFetcher(callFactory))
        .add(FileFetcher(addLastModifiedToFileCacheKey))
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

    override fun execute(request: LoadRequest): RequestDisposable {
        // Start loading the data.
        val job = loaderScope.launch(exceptionHandler) { executeInternal(request) }

        return if (request.target is ViewTarget<*>) {
            val requestId = request.target.view.requestManager.setCurrentRequestJob(job)
            ViewTargetRequestDisposable(requestId, request.target)
        } else {
            BaseTargetRequestDisposable(job)
        }
    }

    override suspend fun execute(request: GetRequest): RequestResult {
        return try {
            withContext(Dispatchers.Main.immediate) { executeInternal(request) }
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Throwable) {
            requestService.errorResult(request, throwable, false)
        }
    }

    @MainThread
    private suspend fun executeInternal(request: Request): SuccessResult {
        // Ensure this image loader isn't shutdown.
        check(!isShutdown) { "The image loader is shutdown." }

        // Create a new event listener.
        val eventListener = eventListenerFactory.create(request)

        // Find the containing lifecycle for this request.
        val lifecycle = requestService.lifecycle(request)

        // Wrap the target to support bitmap pooling.
        val targetDelegate = delegateService.createTargetDelegate(request, eventListener)

        // Wrap the request to manage its lifecycle.
        val requestDelegate = delegateService.createRequestDelegate(coroutineContext.job, targetDelegate, request, lifecycle)

        try {
            // Suspend until the lifecycle is started.
            lifecycle.awaitStarted()

            // Fail before starting if data is null.
            val data = request.data ?: throw NullRequestDataException()

            // Notify the event listener that the request has been dispatched.
            eventListener.onDispatch(request)

            // Invalidate the bitmap if it was provided as input.
            when (data) {
                is BitmapDrawable -> referenceCounter.invalidate(data.bitmap)
                is Bitmap -> referenceCounter.invalidate(data)
            }

            // Add the target as a lifecycle observer if necessary.
            val target = request.target
            if (target is ViewTarget<*> && target is LifecycleObserver) {
                lifecycle.addObserver(target)
            }

            // Prepare to resolve the size lazily.
            val sizeResolver = requestService.sizeResolver(request, context)
            val lazySizeResolver = LazySizeResolver(coroutineContext, sizeResolver, targetDelegate, request, defaults, eventListener)

            // Perform any data mapping.
            eventListener.mapStart(request, data)
            val mappedData = mapData(data, lazySizeResolver)
            eventListener.mapEnd(request, mappedData)

            // Compute the cache key.
            val fetcher = request.validateFetcher(mappedData) ?: registry.requireFetcher(mappedData)
            val cacheKey = request.key?.let { MemoryCache.Key(it) }
                ?: computeCacheKey(fetcher, mappedData, request.parameters, request.transformations, lazySizeResolver)

            // Check the memory cache.
            val memoryCachePolicy = request.memoryCachePolicy ?: defaults.memoryCachePolicy
            val cachedValue = takeIf(memoryCachePolicy.readEnabled) {
                memoryCache.get(cacheKey) ?: request.aliasKeys.firstNotNullIndices { memoryCache.get(MemoryCache.Key(it)) }
            }

            // Ignore the cached bitmap if it is hardware-backed and the request disallows hardware bitmaps.
            val cachedDrawable = cachedValue?.bitmap
                ?.takeIf { requestService.isConfigValidForHardware(request, it.safeConfig) }
                ?.toDrawable(context)

            // Resolve the size and scale.
            val size = lazySizeResolver.size(cachedDrawable)
            val scale = requestService.scale(request, sizeResolver)

            // Short circuit if the cached drawable is valid for the target.
            if (cachedDrawable != null && memoryCacheService
                    .isCachedValueValid(cacheKey, cachedValue, request, sizeResolver, size, scale)) {
                logger?.log(TAG, Log.INFO) { "${Emoji.BRAIN} Cached - $data" }
                val result = SuccessResult(cachedDrawable, DataSource.MEMORY_CACHE)
                targetDelegate.success(result, request.transition ?: defaults.transition)
                eventListener.onSuccess(request, result.source)
                request.listener?.onSuccess(request, result.source)
                return result
            }

            // Fetch and decode the image on a background thread.
            val (drawable, isSampled, source) = loadData(mappedData, fetcher, request, sizeResolver, size, scale, eventListener)

            // Cache the result.
            if (memoryCachePolicy.writeEnabled) {
                memoryCache.put(cacheKey, drawable, isSampled)
            }

            // Set the result on the target.
            logger?.log(TAG, Log.INFO) { "${source.emoji} Successful (${source.name}) - $data" }
            val result = SuccessResult(drawable, source)
            targetDelegate.success(result, request.transition ?: defaults.transition)
            eventListener.onSuccess(request, source)
            request.listener?.onSuccess(request, source)
            return result
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                logger?.log(TAG, Log.INFO) { "${Emoji.CONSTRUCTION} Cancelled - ${request.data}" }
                eventListener.onCancel(request)
                request.listener?.onCancel(request)
            } else {
                logger?.log(TAG, Log.INFO) { "${Emoji.SIREN} Failed - ${request.data} - $throwable" }
                val result = requestService.errorResult(request, throwable, true)
                targetDelegate.error(result, request.transition ?: defaults.transition)
                eventListener.onError(request, throwable)
                request.listener?.onError(request, throwable)
            }
            throw throwable
        } finally {
            requestDelegate.onComplete()
        }
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
    ): MemoryCache.Key? {
        val baseKey = fetcher.key(data) ?: return null

        return if (transformations.isEmpty()) {
            MemoryCache.Key(baseKey, parameters)
        } else {
            // Resolve the size if there are any transformations.
            MemoryCache.Key(baseKey, transformations, lazySizeResolver.size(), parameters)
        }
    }

    /** Load the [mappedData] as a [Drawable]. Apply any [Transformation]s. */
    private suspend inline fun loadData(
        mappedData: Any,
        fetcher: Fetcher<Any>,
        request: Request,
        sizeResolver: SizeResolver,
        size: Size,
        scale: Scale,
        eventListener: EventListener
    ): DrawableResult = withContext(request.dispatcher ?: defaults.dispatcher) {
        val options = requestService.options(request, sizeResolver, size, scale, systemCallbacks.isOnline)

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
                        // Skip decoding the result if we are preloading the data and writing to the memory cache is
                        // disabled. Instead, we exhaust the source and return an empty result.
                        EmptyDecoder
                    } else {
                        request.decoder ?: registry.requireDecoder(request.data!!, fetchResult.source, fetchResult.mimeType)
                    }

                    // Decode the stream.
                    eventListener.decodeStart(request, decoder, options)
                    val decodeResult = decoder.decode(bitmapPool, fetchResult.source, size, options)
                    eventListener.decodeEnd(request, decoder, options)
                    decodeResult
                } catch (rethrown: Exception) {
                    // NOTE: We only close the stream automatically if there is an uncaught exception.
                    // This allows custom decoders to continue to read the source after returning a drawable.
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
        val finalResult = applyTransformations(baseResult, request, size, options, eventListener)
        (finalResult.drawable as? BitmapDrawable)?.bitmap?.prepareToDraw()

        return@withContext finalResult
    }

    /** Apply any [Transformation]s and return an updated [DrawableResult]. */
    @VisibleForTesting
    internal suspend inline fun applyTransformations(
        result: DrawableResult,
        request: Request,
        size: Size,
        options: Options,
        eventListener: EventListener
    ): DrawableResult {
        val transformations = request.transformations
        if (transformations.isEmpty()) return result

        // Convert the drawable into a bitmap with a valid config.
        eventListener.transformStart(request)
        val baseBitmap = if (result.drawable is BitmapDrawable) {
            val resultBitmap = result.drawable.bitmap
            if (resultBitmap.safeConfig in RequestService.VALID_TRANSFORMATION_CONFIGS) {
                resultBitmap
            } else {
                logger?.log(TAG, Log.INFO) {
                    "Converting bitmap with config ${resultBitmap.safeConfig} to apply transformations: $transformations"
                }
                drawableDecoder.convert(result.drawable, options.config, size, options.scale, options.allowInexactSize)
            }
        } else {
            logger?.log(TAG, Log.INFO) {
                "Converting drawable of type ${result.drawable::class.java.canonicalName} " +
                    "to apply transformations: $transformations"
            }
            drawableDecoder.convert(result.drawable, options.config, size, options.scale, options.allowInexactSize)
        }
        val transformedBitmap = transformations.foldIndices(baseBitmap) { bitmap, transformation ->
            transformation.transform(bitmapPool, bitmap, size).also { coroutineContext.ensureActive() }
        }
        val transformedResult = result.copy(drawable = transformedBitmap.toDrawable(context))
        eventListener.transformEnd(request)
        return transformedResult
    }

    override fun invalidate(key: String) {
        val cacheKey = MemoryCache.Key(key)
        memoryCache.invalidate(cacheKey)
        weakMemoryCache.invalidate(cacheKey)
    }

    /** Called by [SystemCallbacks.onTrimMemory]. */
    fun onTrimMemory(level: Int) {
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
        systemCallbacks.shutdown()
        clearMemory()
    }

    /** Lazily resolves and caches a request's size. */
    @VisibleForTesting
    internal class LazySizeResolver(
        private val coroutineContext: CoroutineContext,
        private val sizeResolver: SizeResolver,
        private val targetDelegate: TargetDelegate,
        private val request: Request,
        private val defaults: DefaultRequestOptions,
        private val eventListener: EventListener
    ) {

        private var size: Size? = null

        /** Calls [TargetDelegate.start], [SizeResolver.size], and caches the resolved size. */
        @MainThread
        suspend inline fun size(cached: BitmapDrawable? = null): Size {
            // Return the size if it has already been resolved.
            size?.let { return it }

            beforeResolveSize(cached)
            val size = sizeResolver.size().also { size = it }
            afterResolveSize(size)
            return size
        }

        /** Called immediately before [SizeResolver.size]. */
        private fun beforeResolveSize(cached: BitmapDrawable?) {
            targetDelegate.start(cached, cached ?: request.placeholderOrDefault(defaults))
            eventListener.onStart(request)
            request.listener?.onStart(request)
            eventListener.resolveSizeStart(request)
        }

        /** Called immediately after [SizeResolver.size]. */
        private fun afterResolveSize(size: Size) {
            eventListener.resolveSizeEnd(request, size)
            coroutineContext.ensureActive()
        }
    }

    companion object {
        private const val TAG = "RealImageLoader"
    }
}
