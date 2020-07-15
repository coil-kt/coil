package coil

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.annotation.MainThread
import coil.annotation.ExperimentalCoilApi
import coil.bitmappool.BitmapPool
import coil.decode.BitmapFactoryDecoder
import coil.decode.DrawableDecoderService
import coil.fetch.AssetUriFetcher
import coil.fetch.BitmapFetcher
import coil.fetch.ContentUriFetcher
import coil.fetch.DrawableFetcher
import coil.fetch.FileFetcher
import coil.fetch.HttpUrlFetcher
import coil.fetch.ResourceUriFetcher
import coil.interceptor.EngineInterceptor
import coil.interceptor.RealInterceptorChain
import coil.map.FileUriMapper
import coil.map.HttpUriMapper
import coil.map.ResourceIntMapper
import coil.map.ResourceUriMapper
import coil.map.StringMapper
import coil.memory.BitmapReferenceCounter
import coil.memory.DelegateService
import coil.memory.MemoryCache
import coil.memory.RealMemoryCache
import coil.memory.RequestService
import coil.memory.StrongMemoryCache
import coil.memory.TargetDelegate
import coil.memory.WeakMemoryCache
import coil.request.BaseTargetRequestDisposable
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.NullRequestDataException
import coil.request.RequestDisposable
import coil.request.RequestResult
import coil.request.SuccessResult
import coil.request.ViewTargetRequestDisposable
import coil.size.Size
import coil.size.SizeResolver
import coil.target.ViewTarget
import coil.util.Emoji
import coil.util.Logger
import coil.util.SystemCallbacks
import coil.util.Utils.REQUEST_TYPE_ENQUEUE
import coil.util.Utils.REQUEST_TYPE_EXECUTE
import coil.util.awaitStarted
import coil.util.emoji
import coil.util.getLifecycle
import coil.util.job
import coil.util.log
import coil.util.requestManager
import coil.util.toDrawable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalCoilApi::class)
internal class RealImageLoader(
    context: Context,
    override val defaults: DefaultRequestOptions,
    override val bitmapPool: BitmapPool,
    referenceCounter: BitmapReferenceCounter,
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
    private val drawableDecoder = DrawableDecoderService(bitmapPool)
    private val systemCallbacks = SystemCallbacks(this, context)
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
        .add(DrawableFetcher(context, drawableDecoder))
        .add(BitmapFetcher(context))
        // Decoders
        .add(BitmapFactoryDecoder(context))
        .build()
    private val interceptors = registry.interceptors + newEngineInterceptor()
    private val isShutdown = AtomicBoolean(false)

    override fun enqueue(request: ImageRequest): RequestDisposable {
        val job = scope.launch {
            val result = executeMain(request, REQUEST_TYPE_ENQUEUE)
            if (result is ErrorResult) throw result.throwable
        }

        return if (request.target is ViewTarget<*>) {
            val requestId = request.target.view.requestManager.setCurrentRequestJob(job)
            ViewTargetRequestDisposable(requestId, request.target)
        } else {
            BaseTargetRequestDisposable(job)
        }
    }

    override suspend fun execute(request: ImageRequest): RequestResult {
        if (request.target is ViewTarget<*>) {
            request.target.view.requestManager.setCurrentRequestJob(coroutineContext.job)
        }

        return withContext(Dispatchers.Main.immediate) {
            executeMain(request, REQUEST_TYPE_EXECUTE)
        }
    }

    @MainThread
    private suspend fun executeMain(request: ImageRequest, type: Int): RequestResult {
        // Ensure this image loader isn't shutdown.
        check(!isShutdown.get()) { "The image loader is shutdown." }

        // Create a new event listener.
        val eventListener = eventListenerFactory.create(request)

        // Find the containing lifecycle for this request.
        val lifecycle = request.getLifecycle()

        // Wrap the target to support bitmap pooling.
        val targetDelegate = delegateService.createTargetDelegate(request.target, type, eventListener)

        // Wrap the request to manage its lifecycle.
        val requestDelegate = delegateService.createRequestDelegate(request, targetDelegate, lifecycle, coroutineContext.job)

        try {
            // Fail before starting if data is null.
            request.data ?: throw NullRequestDataException()

            // Enqueued requests suspend until the lifecycle is started.
            if (type == REQUEST_TYPE_ENQUEUE) lifecycle.awaitStarted()

            // Set the placeholder on the target.
            val cached = checkMemoryCache(request.context, request.placeholderKey)
            targetDelegate.start(cached, cached ?: request.placeholder ?: defaults.placeholder)
            eventListener.onStart(request)
            request.listener?.onStart(request)

            // Resolve the size.
            val sizeResolver = requestService.sizeResolver(request)
            eventListener.resolveSizeStart(request, sizeResolver)
            val size = sizeResolver.size()
            eventListener.resolveSizeEnd(request, sizeResolver, size)

            // Execute the interceptor chain.
            val result = executeChain(request, type, size, sizeResolver, eventListener)

            // Set the result on the target.
            when (result) {
                is SuccessResult -> onSuccess(result, targetDelegate, eventListener)
                is ErrorResult -> onError(result, targetDelegate, eventListener)
            }
            return result
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                onCancel(request, eventListener)
                throw throwable
            } else {
                // Create the default error result if there's an uncaught exception.
                val result = requestService.errorResult(request, throwable)
                onError(result, targetDelegate, eventListener)
                return result
            }
        } finally {
            requestDelegate.complete()
        }
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

    private suspend inline fun executeChain(
        request: ImageRequest,
        type: Int,
        size: Size,
        sizeResolver: SizeResolver,
        eventListener: EventListener
    ): RequestResult = withContext(request.dispatcher ?: defaults.dispatcher) {
        RealInterceptorChain(request, type, interceptors, 0, request, defaults, size, sizeResolver, eventListener).proceed(request)
    }

    private suspend inline fun onSuccess(
        result: SuccessResult,
        targetDelegate: TargetDelegate,
        eventListener: EventListener
    ) {
        val request = result.request
        val metadata = result.metadata
        val dataSource = metadata.dataSource
        logger?.log(TAG, Log.INFO) { "${dataSource.emoji} Successful (${dataSource.name}) - ${request.data}" }
        targetDelegate.success(result, request.transition ?: defaults.transition)
        eventListener.onSuccess(request, metadata)
        request.listener?.onSuccess(request, metadata)
    }

    private suspend inline fun onError(
        result: ErrorResult,
        targetDelegate: TargetDelegate,
        eventListener: EventListener
    ) {
        val request = result.request
        logger?.log(TAG, Log.INFO) { "${Emoji.SIREN} Failed - ${request.data} - ${result.throwable}" }
        targetDelegate.error(result, request.transition ?: defaults.transition)
        eventListener.onError(request, result.throwable)
        request.listener?.onError(request, result.throwable)
    }

    private fun onCancel(request: ImageRequest, eventListener: EventListener) {
        logger?.log(TAG, Log.INFO) { "${Emoji.CONSTRUCTION} Cancelled - ${request.data}" }
        eventListener.onCancel(request)
        request.listener?.onCancel(request)
    }

    private fun newEngineInterceptor(): EngineInterceptor {
        return EngineInterceptor(registry, bitmapPool, strongMemoryCache, weakMemoryCache, requestService,
            systemCallbacks, drawableDecoder, logger)
    }

    private fun checkMemoryCache(context: Context, key: MemoryCache.Key?): BitmapDrawable? {
        return key?.let { strongMemoryCache.get(it) ?: weakMemoryCache.get(it) }?.bitmap?.toDrawable(context)
    }

    companion object {
        private const val TAG = "RealImageLoader"
    }
}
