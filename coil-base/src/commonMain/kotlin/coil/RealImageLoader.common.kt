package coil

import coil.annotation.MainThread
import coil.disk.DiskCache
import coil.fetch.ByteArrayFetcher
import coil.fetch.PathFetcher
import coil.intercept.EngineInterceptor
import coil.intercept.RealInterceptorChain
import coil.key.FileUriKeyer
import coil.key.GenericUriKeyer
import coil.map.PathMapper
import coil.map.StringMapper
import coil.memory.MemoryCache
import coil.request.Disposable
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.NullRequestData
import coil.request.NullRequestDataException
import coil.request.RequestService
import coil.request.SuccessResult
import coil.target.Target
import coil.util.Logger
import coil.util.SystemCallbacks
import coil.util.emoji
import coil.util.get
import coil.util.log
import kotlin.coroutines.coroutineContext
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext

internal class RealImageLoader(
    val options: Options,
) : ImageLoader {

    private val scope = CoroutineScope(options.logger)
    private val systemCallbacks = SystemCallbacks(options)
    private val requestService = RequestService(this, systemCallbacks, options.logger)
    override val defaults get() = options.defaults
    override val memoryCache by options.memoryCacheLazy
    override val diskCache by options.diskCacheLazy
    override val components = options.componentRegistry.newBuilder()
        .addAndroidComponents(options)
        .addJvmComponents(options)
        .addCommonComponents(options)
        .add(EngineInterceptor(this, requestService, options.logger))
        .build()
    private val shutdown = atomic(false)

    init {
        // Must be called after the image loader is fully initialized.
        systemCallbacks.register(this)
    }

    override fun enqueue(request: ImageRequest): Disposable {
        // Start executing the request on the main thread.
        val job = scope.async {
            executeMain(request, REQUEST_TYPE_ENQUEUE).also { result ->
                if (result is ErrorResult) options.logger?.log(TAG, result.throwable)
            }
        }

        // Update the current request attached to the view and return a new disposable.
        return getDisposable(request, job)
    }

    override suspend fun execute(request: ImageRequest) = coroutineScope {
        // Start executing the request on the main thread.
        val job = async(Dispatchers.Main.immediate) {
            executeMain(request, REQUEST_TYPE_EXECUTE)
        }

        // Update the current request attached to the view and await the result.
        return@coroutineScope getDisposable(request, job).job.await()
    }

    @MainThread
    private suspend fun executeMain(initialRequest: ImageRequest, type: Int): ImageResult {
        // Wrap the request to manage its lifecycle.
        val requestDelegate = requestService.requestDelegate(initialRequest, coroutineContext.job)
        requestDelegate.assertActive()

        // Apply this image loader's defaults to this request.
        val request = initialRequest.newBuilder().defaults(defaults).build()

        // Create a new event listener.
        val eventListener = options.eventListenerFactory.create(request)

        try {
            // Fail before starting if data is null.
            if (request.data == NullRequestData) {
                throw NullRequestDataException()
            }

            // Set up the request's lifecycle observers.
            requestDelegate.start()

            // Enqueued requests suspend until the lifecycle is started.
            if (type == REQUEST_TYPE_ENQUEUE) {
                awaitLifecycleStarted(request)
            }

            // Set the placeholder on the target.
            val cachedPlaceholder = memoryCache?.get(request.placeholderMemoryCacheKey)?.image
            val placeholder = cachedPlaceholder ?: request.placeholderFactory()
            request.target?.onStart(placeholder)
            eventListener.onStart(request)
            request.listener?.onStart(request)

            // Resolve the size.
            eventListener.resolveSizeStart(request)
            val size = request.sizeResolver.size()
            eventListener.resolveSizeEnd(request, size)

            // Execute the interceptor chain.
            val result = withContext(request.interceptorDispatcher) {
                RealInterceptorChain(
                    initialRequest = request,
                    interceptors = components.interceptors,
                    index = 0,
                    request = request,
                    size = size,
                    eventListener = eventListener,
                    isPlaceholderCached = cachedPlaceholder != null,
                ).proceed()
            }

            // Set the result on the target.
            when (result) {
                is SuccessResult -> onSuccess(result, request.target, eventListener)
                is ErrorResult -> onError(result, request.target, eventListener)
            }
            return result
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                onCancel(request, eventListener)
                throw throwable
            } else {
                // Create the default error result if there's an uncaught exception.
                val result = requestService.errorResult(request, throwable)
                onError(result, request.target, eventListener)
                return result
            }
        } finally {
            requestDelegate.complete()
        }
    }

    override fun shutdown() {
        if (shutdown.getAndSet(true)) return
        scope.cancel()
        systemCallbacks.shutdown()
        memoryCache?.clear()
    }

    override fun newBuilder() = ImageLoader.Builder(options)

    private fun onSuccess(
        result: SuccessResult,
        target: Target?,
        eventListener: EventListener,
    ) {
        val request = result.request
        val dataSource = result.dataSource
        options.logger?.log(TAG, Logger.Level.Info) {
            "${dataSource.emoji} Successful (${dataSource.name}) - ${request.data}"
        }
        transition(result, target, eventListener) {
            target?.onSuccess(result.image)
        }
        eventListener.onSuccess(request, result)
        request.listener?.onSuccess(request, result)
    }

    private fun onError(
        result: ErrorResult,
        target: Target?,
        eventListener: EventListener,
    ) {
        val request = result.request
        options.logger?.log(TAG, Logger.Level.Info) {
            "🚨 Failed - ${request.data} - ${result.throwable}"
        }
        transition(result, target, eventListener) {
            target?.onError(result.image)
        }
        eventListener.onError(request, result)
        request.listener?.onError(request, result)
    }

    private fun onCancel(
        request: ImageRequest,
        eventListener: EventListener,
    ) {
        options.logger?.log(TAG, Logger.Level.Info) {
            "🏗 Cancelled - ${request.data}"
        }
        eventListener.onCancel(request)
        request.listener?.onCancel(request)
    }

    data class Options(
        val application: PlatformContext,
        val defaults: ImageRequest.Defaults,
        val memoryCacheLazy: Lazy<MemoryCache?>,
        val diskCacheLazy: Lazy<DiskCache?>,
        val eventListenerFactory: EventListener.Factory,
        val componentRegistry: ComponentRegistry,
        val logger: Logger?,
        val extras: Extras,
    )
}

private fun CoroutineScope(logger: Logger?): CoroutineScope {
    val context = SupervisorJob() +
        Dispatchers.Main.immediate +
        CoroutineExceptionHandler { _, throwable -> logger?.log(TAG, throwable) }
    return CoroutineScope(context)
}

internal expect fun getDisposable(
    request: ImageRequest,
    job: Deferred<ImageResult>,
): Disposable

internal expect suspend fun awaitLifecycleStarted(
    request: ImageRequest,
)

internal expect inline fun transition(
    result: ImageResult,
    target: Target?,
    eventListener: EventListener,
    setDrawable: () -> Unit,
)

internal expect fun ComponentRegistry.Builder.addAndroidComponents(
    options: RealImageLoader.Options,
): ComponentRegistry.Builder

internal expect fun ComponentRegistry.Builder.addJvmComponents(
    options: RealImageLoader.Options,
): ComponentRegistry.Builder

internal fun ComponentRegistry.Builder.addCommonComponents(
    options: RealImageLoader.Options,
): ComponentRegistry.Builder {
    return this
        // Mappers
        .add(StringMapper())
        .add(PathMapper())
        // Keyers
        .add(FileUriKeyer(options.addLastModifiedToFileCacheKey))
        .add(GenericUriKeyer())
        // Fetchers
        .add(ByteArrayFetcher.Factory())
        .add(PathFetcher.Factory())
}

private const val TAG = "RealImageLoader"
private const val REQUEST_TYPE_ENQUEUE = 0
private const val REQUEST_TYPE_EXECUTE = 1
