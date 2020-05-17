package coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleObserver
import coil.annotation.ExperimentalCoilApi
import coil.bitmappool.BitmapPool
import coil.decode.BitmapFactoryDecoder
import coil.decode.DataSource
import coil.decode.DrawableDecoderService
import coil.fetch.AssetUriFetcher
import coil.fetch.BitmapFetcher
import coil.fetch.ContentUriFetcher
import coil.fetch.DrawableFetcher
import coil.fetch.Fetcher
import coil.fetch.FileFetcher
import coil.fetch.HttpUriFetcher
import coil.fetch.HttpUrlFetcher
import coil.fetch.ResourceUriFetcher
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
import coil.request.RequestExecutor
import coil.request.RequestResult
import coil.request.SuccessResult
import coil.request.ViewTargetRequestDisposable
import coil.size.Size
import coil.size.SizeResolver
import coil.target.ViewTarget
import coil.transform.Transformation
import coil.util.Emoji
import coil.util.Logger
import coil.util.SystemCallbacks
import coil.util.emoji
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
    internal val logger: Logger?
) : ImageLoader {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
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
        .add(FileFetcher())
        .add(AssetUriFetcher(context))
        .add(ContentUriFetcher(context))
        .add(ResourceUriFetcher(context, drawableDecoder))
        .add(DrawableFetcher(context, drawableDecoder))
        .add(BitmapFetcher(context))
        // Decoders
        .add(BitmapFactoryDecoder(context))
        .build()

    private val requestExecutor = RequestExecutor(context, defaults, bitmapPool, requestService, drawableDecoder, systemCallbacks, registry, logger)

    // isShutdown is only accessed from the main thread.
    private var isShutdown = false

    override fun execute(request: LoadRequest): RequestDisposable {
        // Start loading the data.
        val job = coroutineScope.launch(exceptionHandler) { executeInternal(request) }

        return if (request.target is ViewTarget<*>) {
            val requestId = request.target.view.requestManager.setCurrentRequestJob(job)
            ViewTargetRequestDisposable(requestId, request.target)
        } else {
            BaseTargetRequestDisposable(job)
        }
    }

    override suspend fun execute(request: GetRequest): RequestResult {
        return try {
            executeInternal(request)
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Throwable) {
            requestService.errorResult(request, throwable, false)
        }
    }

    private suspend fun executeInternal(request: Request) = withContext(Dispatchers.Main.immediate) outerJob@{
        // Ensure this image loader isn't shutdown.
        check(!isShutdown) { "The image loader is shutdown." }

        // Create a new event listener.
        val eventListener = eventListenerFactory.create(request)

        // Compute lifecycle info on the main thread.
        val (lifecycle, mainDispatcher) = requestService.lifecycleInfo(request)

        // Wrap the target to support bitmap pooling.
        val targetDelegate = delegateService.createTargetDelegate(request, eventListener)

        val deferred = async(mainDispatcher, CoroutineStart.LAZY) innerJob@{
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
            val lazySizeResolver = LazySizeResolver(this, sizeResolver, targetDelegate, request, defaults, eventListener)

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
                memoryCache.getValue(cacheKey) ?: request.aliasKeys.firstNotNullIndices { memoryCache.getValue(MemoryCache.Key(it)) }
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
            if (cachedDrawable != null && memoryCacheService
                    .isCachedValueValid(cacheKey, cachedValue, request, sizeResolver, size, scale)) {
                logger?.log(TAG, Log.INFO) { "${Emoji.BRAIN} Cached - $data" }
                val result = SuccessResult(cachedDrawable, DataSource.MEMORY_CACHE)
                targetDelegate.success(result, request.transition ?: defaults.transition)
                eventListener.onSuccess(request, result.source)
                request.listener?.onSuccess(request, result.source)
                return@innerJob result
            }

            // Fetch and decode the image.
            val (drawable, isSampled, source) = requestExecutor.loadData(mappedData, fetcher, request, sizeResolver, size, scale, eventListener)

            // Cache the result.
            if (memoryCachePolicy.writeEnabled) {
                memoryCache.putValue(cacheKey, drawable, isSampled)
            }

            // Set the result on the target.
            logger?.log(TAG, Log.INFO) { "${source.emoji} Successful (${source.name}) - $data" }
            val result = SuccessResult(drawable, source)
            targetDelegate.success(result, request.transition ?: defaults.transition)
            eventListener.onSuccess(request, source)
            request.listener?.onSuccess(request, source)

            return@innerJob result
        }

        // Wrap the request to manage its lifecycle.
        val requestDelegate = delegateService.createRequestDelegate(request, targetDelegate, lifecycle, mainDispatcher, deferred)

        deferred.invokeOnCompletion { throwable ->
            // Ensure callbacks are executed on the main thread.
            coroutineScope.launch(Dispatchers.Main.immediate) {
                requestDelegate.onComplete()
                throwable ?: return@launch

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
    ): MemoryCache.Key? {
        val baseKey = fetcher.key(data) ?: return null

        return if (transformations.isEmpty()) {
            MemoryCache.Key(baseKey, parameters)
        } else {
            // Resolve the size if there are any transformations.
            MemoryCache.Key(baseKey, transformations, lazySizeResolver.size(), parameters)
        }
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

        coroutineScope.cancel()
        systemCallbacks.shutdown()
        clearMemory()
    }

    /** Lazily resolves and caches a request's size. */
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

        /**
         * Calls [TargetDelegate.start], [SizeResolver.size], and caches the resolved size.
         *
         * This method is inlined as long as it is called from inside [RealImageLoader].
         * [beforeResolveSize] and [afterResolveSize] are outlined to reduce the amount of inlined code.
         */
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
            scope.ensureActive()
        }
    }

    companion object {
        private const val TAG = "RealImageLoader"
    }
}
