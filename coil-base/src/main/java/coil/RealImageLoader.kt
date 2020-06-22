package coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
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
import coil.map.ResourceIntMapper
import coil.map.ResourceUriMapper
import coil.map.StringMapper
import coil.memory.BitmapReferenceCounter
import coil.memory.DelegateService
import coil.memory.MemoryCache
import coil.memory.MemoryCacheService
import coil.memory.RealMemoryCache
import coil.memory.RequestService
import coil.memory.StrongMemoryCache
import coil.memory.TargetDelegate
import coil.memory.WeakMemoryCache
import coil.request.BaseTargetRequestDisposable
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.Metadata
import coil.request.NullRequestDataException
import coil.request.RequestDisposable
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
import coil.util.Utils.REQUEST_TYPE_ENQUEUE
import coil.util.Utils.REQUEST_TYPE_EXECUTE
import coil.util.awaitStarted
import coil.util.closeQuietly
import coil.util.emoji
import coil.util.fetcher
import coil.util.foldIndices
import coil.util.getLifecycle
import coil.util.invoke
import coil.util.job
import coil.util.log
import coil.util.mapData
import coil.util.requestManager
import coil.util.requireDecoder
import coil.util.requireFetcher
import coil.util.safeConfig
import coil.util.set
import coil.util.takeIf
import coil.util.toDrawable
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
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalCoilApi::class)
internal class RealImageLoader(
    private val context: Context,
    override val defaults: DefaultRequestOptions,
    override val bitmapPool: BitmapPool,
    private val referenceCounter: BitmapReferenceCounter,
    private val strongMemoryCache: StrongMemoryCache,
    private val weakMemoryCache: WeakMemoryCache,
    callFactory: Call.Factory,
    private val eventListenerFactory: EventListener.Factory,
    registry: ComponentRegistry,
    val logger: Logger?
) : ImageLoader {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate +
        CoroutineExceptionHandler { _, throwable -> logger?.log(TAG, throwable) })
    private val delegateService = DelegateService(this, referenceCounter, logger)
    private val requestService = RequestService(defaults, logger)
    override val memoryCache = RealMemoryCache(strongMemoryCache, weakMemoryCache, referenceCounter)
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
    private val isShutdown = AtomicBoolean(false)

    override fun enqueue(request: ImageRequest): RequestDisposable {
        val job = scope.launch {
            val result = execute(request, REQUEST_TYPE_ENQUEUE)
            if (result is ErrorResult) throw result.throwable
        }

        return if (request.target is ViewTarget<*>) {
            val requestId = request.target.view.requestManager.setCurrentRequestJob(job)
            ViewTargetRequestDisposable(requestId, request.target)
        } else {
            BaseTargetRequestDisposable(job)
        }
    }

    override suspend fun execute(request: ImageRequest): ImageResult {
        if (request.target is ViewTarget<*>) {
            request.target.view.requestManager.setCurrentRequestJob(coroutineContext.job)
        }

        return withContext(Dispatchers.Main.immediate) {
            execute(request, REQUEST_TYPE_EXECUTE)
        }
    }

    @MainThread
    private suspend fun execute(request: ImageRequest, type: Int): ImageResult {
        // Ensure this image loader isn't shutdown.
        check(!isShutdown.get()) { "The image loader is shutdown." }

        // Create a new event listener.
        val eventListener = eventListenerFactory.create(request)

        // Find the containing lifecycle for this request.
        val lifecycle = request.getLifecycle()

        // Wrap the target to support bitmap pooling.
        val targetDelegate = delegateService.createTargetDelegate(request, type, eventListener)

        // Wrap the request to manage its lifecycle.
        val requestDelegate = delegateService.createRequestDelegate(coroutineContext.job, targetDelegate, request, lifecycle)

        try {
            // Fail before starting if data is null.
            val data = request.data ?: throw NullRequestDataException()

            // Enqueued requests suspend until the lifecycle is started.
            if (type == REQUEST_TYPE_ENQUEUE) lifecycle.awaitStarted()

            // Notify the event listener that the request has been dispatched.
            eventListener.onDispatch(request)

            // Prepare to resolve the size.
            val sizeResolver = requestService.sizeResolver(request, context)
            val scale = requestService.scale(request, sizeResolver)
            val lazySizeResolver = LazySizeResolver(sizeResolver, targetDelegate, request, defaults, eventListener)

            // Invalidate the bitmap if it was provided as input.
            when (data) {
                is BitmapDrawable -> referenceCounter.invalidate(data.bitmap)
                is Bitmap -> referenceCounter.invalidate(data)
            }

            // Perform any data mapping.
            eventListener.mapStart(request, data)
            val mappedData = registry.mapData(data) { lazySizeResolver.size() }
            eventListener.mapEnd(request, mappedData)

            // Check the memory cache.
            val fetcher = request.fetcher(mappedData) ?: registry.requireFetcher(mappedData)
            val key = request.key ?: computeKey(request, mappedData, fetcher) { lazySizeResolver.size() }
            val memoryCachePolicy = request.memoryCachePolicy ?: defaults.memoryCachePolicy
            val value = takeIf(memoryCachePolicy.readEnabled) {
                key?.let { strongMemoryCache.get(it) ?: weakMemoryCache.get(it) }
            }

            // Ignore the cached bitmap if it is hardware-backed and the request disallows hardware bitmaps.
            val cachedDrawable = value?.bitmap
                ?.takeIf { requestService.isConfigValidForHardware(request, it.safeConfig) }
                ?.toDrawable(context)

            // Resolve the size if it has not been resolved already.
            val size = lazySizeResolver.size(cachedDrawable)

            // Short circuit if the cached bitmap is valid.
            if (cachedDrawable != null && memoryCacheService.isCachedValueValid(key, value, request, sizeResolver, size, scale)) {
                return SuccessResult(
                    drawable = value.bitmap.toDrawable(context),
                    metadata = Metadata(key, value.isSampled, DataSource.MEMORY_CACHE)
                )
            }

            // Fetch and decode the image.
            val (drawable, isSampled, dataSource) = fetchAndDecode(mappedData, fetcher, request, type,
                memoryCachePolicy, sizeResolver, size, scale, eventListener)

            // Cache the result in the memory cache.
            val isCached = memoryCachePolicy.writeEnabled && strongMemoryCache.set(key, drawable, isSampled)

            // Set the result on the target.
            logger?.log(TAG, Log.INFO) { "${dataSource.emoji} Successful (${dataSource.name}) - ${request.data}" }
            val result = SuccessResult(drawable, Metadata(key.takeIf { isCached }, isSampled, dataSource))
            targetDelegate.success(result, request.transition ?: defaults.transition)
            eventListener.onSuccess(request, result.metadata)
            request.listener?.onSuccess(request, result.metadata)
            return result
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                logger?.log(TAG, Log.INFO) { "${Emoji.CONSTRUCTION} Cancelled - ${request.data}" }
                eventListener.onCancel(request)
                request.listener?.onCancel(request)
                throw throwable
            } else {
                logger?.log(TAG, Log.INFO) { "${Emoji.SIREN} Failed - ${request.data} - $throwable" }
                val result = requestService.errorResult(request, throwable)
                targetDelegate.error(result, request.transition ?: defaults.transition)
                eventListener.onError(request, result.throwable)
                request.listener?.onError(request, result.throwable)
                return result
            }
        } finally {
            requestDelegate.complete()
        }
    }

    /** Compute the complex cache key for this request. */
    @VisibleForTesting
    internal inline fun computeKey(
        request: ImageRequest,
        data: Any,
        fetcher: Fetcher<Any>,
        lazySize: () -> Size
    ): MemoryCache.Key? {
        val base = fetcher.key(data) ?: return null
        return if (request.transformations.isEmpty()) {
            MemoryCache.Key(base, request.parameters)
        } else {
            MemoryCache.Key(base, request.transformations, lazySize(), request.parameters)
        }
    }

    /** Load the [data] as a [Drawable]. Apply any [Transformation]s. */
    @VisibleForTesting
    internal suspend inline fun fetchAndDecode(
        data: Any,
        fetcher: Fetcher<Any>,
        request: ImageRequest,
        type: Int,
        memoryCachePolicy: CachePolicy,
        sizeResolver: SizeResolver,
        size: Size,
        scale: Scale,
        eventListener: EventListener
    ): DrawableResult = withContext(request.dispatcher ?: defaults.dispatcher) {
        val options = requestService.options(request, sizeResolver, size, scale, systemCallbacks.isOnline)

        eventListener.fetchStart(request, fetcher, options)
        val fetchResult = fetcher.fetch(bitmapPool, data, size, options)
        eventListener.fetchEnd(request, fetcher, options, fetchResult)

        val baseResult = when (fetchResult) {
            is SourceResult -> {
                val decodeResult = try {
                    // Check if we're cancelled.
                    coroutineContext.ensureActive()

                    // Find the relevant decoder.
                    val isDiskOnlyPreload = type == REQUEST_TYPE_ENQUEUE && request.target == null && !memoryCachePolicy.writeEnabled
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
                    eventListener.decodeEnd(request, decoder, options, decodeResult)
                    decodeResult
                } catch (throwable: Throwable) {
                    // Only close the stream automatically if there is an uncaught exception.
                    // This allows custom decoders to continue to read the source after returning a drawable.
                    fetchResult.source.closeQuietly()
                    throw throwable
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
        coroutineContext.ensureActive()

        // Apply any transformations and prepare to draw.
        val finalResult = transform(baseResult, request, size, options, eventListener)
        (finalResult.drawable as? BitmapDrawable)?.bitmap?.prepareToDraw()
        finalResult
    }

    /** Apply any [Transformation]s and return an updated [DrawableResult]. */
    @VisibleForTesting
    internal suspend inline fun transform(
        result: DrawableResult,
        request: ImageRequest,
        size: Size,
        options: Options,
        eventListener: EventListener
    ): DrawableResult {
        val transformations = request.transformations
        if (transformations.isEmpty()) return result

        // Convert the drawable into a bitmap with a valid config.
        val input = if (result.drawable is BitmapDrawable) {
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
        eventListener.transformStart(request, input)
        val output = transformations.foldIndices(input) { bitmap, transformation ->
            transformation.transform(bitmapPool, bitmap, size).also { coroutineContext.ensureActive() }
        }
        eventListener.transformEnd(request, output)
        return result.copy(drawable = output.toDrawable(context))
    }

    /** Called by [SystemCallbacks.onTrimMemory]. */
    fun onTrimMemory(level: Int) {
        strongMemoryCache.trimMemory(level)
        weakMemoryCache.trimMemory(level)
        bitmapPool.trimMemory(level)
    }

    override fun shutdown() {
        if (isShutdown.getAndSet(true)) return

        // Order is important.
        scope.cancel()
        systemCallbacks.shutdown()
        strongMemoryCache.clearMemory()
        weakMemoryCache.clearMemory()
        bitmapPool.clear()
    }

    /** Lazily resolves and caches a request's size. */
    @VisibleForTesting
    internal class LazySizeResolver(
        private val sizeResolver: SizeResolver,
        private val targetDelegate: TargetDelegate,
        private val request: ImageRequest,
        private val defaults: DefaultRequestOptions,
        private val eventListener: EventListener
    ) {

        private var size: Size? = null

        /** Calls [TargetDelegate.start], [SizeResolver.size], and caches the resolved size. */
        @MainThread
        suspend inline fun size(cached: BitmapDrawable? = null): Size {
            // Return the size if it has already been resolved.
            size?.let { return it }

            targetDelegate.start(cached, cached ?: request.placeholder ?: defaults.placeholder)
            eventListener.onStart(request)
            request.listener?.onStart(request)

            eventListener.resolveSizeStart(request, sizeResolver)
            val size = sizeResolver.size().also { size = it }
            eventListener.resolveSizeEnd(request, sizeResolver, size)

            coroutineContext.ensureActive()

            return size
        }
    }

    companion object {
        private const val TAG = "RealImageLoader"
    }
}
