package coil3

import coil3.disk.DiskCache
import coil3.fetch.ByteArrayFetcher
import coil3.fetch.DataUriFetcher
import coil3.fetch.FileUriFetcher
import coil3.intercept.EngineInterceptor
import coil3.intercept.RealInterceptorChain
import coil3.key.FileUriKeyer
import coil3.key.UriKeyer
import coil3.map.PathMapper
import coil3.map.StringMapper
import coil3.memory.MemoryCache
import coil3.request.Disposable
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.NullRequestData
import coil3.request.NullRequestDataException
import coil3.request.RequestService
import coil3.request.SuccessResult
import coil3.target.Target
import coil3.util.ErrorResult
import coil3.util.FetcherServiceLoaderTarget
import coil3.util.Logger
import coil3.util.ServiceLoaderComponentRegistry
import coil3.util.SystemCallbacks
import coil3.util.emoji
import coil3.util.log
import coil3.util.mapNotNullIndices
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
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
    private val systemCallbacks = SystemCallbacks(this)
    private val requestService = RequestService(this, systemCallbacks, options.logger)
    override val defaults get() = options.defaults
    override val memoryCache by options.memoryCacheLazy
    override val diskCache by options.diskCacheLazy
    override val components = options.componentRegistry.newBuilder()
        .addServiceLoaderComponents(options)
        .addAndroidComponents(options)
        .addJvmComponents(options)
        .addAppleComponents(options)
        .addCommonComponents()
        .add(EngineInterceptor(this, systemCallbacks, requestService, options.logger))
        .build()
    private val shutdown = atomic(false)

    override fun enqueue(request: ImageRequest): Disposable {
        // Start executing the request on the main thread.
        val job = scope.async(options.mainCoroutineContextLazy.value) {
            execute(request, REQUEST_TYPE_ENQUEUE)
        }

        // Update the current request attached to the view and return a new disposable.
        return getDisposable(request, job)
    }

    override suspend fun execute(request: ImageRequest): ImageResult {
        if (!needsExecuteOnMainDispatcher(request)) {
            // Fast path: skip dispatching.
            return execute(request, REQUEST_TYPE_EXECUTE)
        } else {
            // Slow path: dispatch to the main thread.
            return coroutineScope {
                // Start executing the request on the main thread.
                val job = async(options.mainCoroutineContextLazy.value) {
                    execute(request, REQUEST_TYPE_EXECUTE)
                }

                // Update the current request attached to the view and await the result.
                getDisposable(request, job).job.await()
            }
        }
    }

    private suspend fun execute(initialRequest: ImageRequest, type: Int): ImageResult {
        // Wrap the request to manage its lifecycle.
        val requestDelegate = requestService.requestDelegate(
            request = initialRequest,
            job = coroutineContext.job,
            findLifecycle = type == REQUEST_TYPE_ENQUEUE,
        ).apply { assertActive() }

        // Apply this image loader's defaults and other configuration to this request.
        val request = requestService.updateRequest(initialRequest)

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
                requestDelegate.awaitStarted()
            }

            // Set the placeholder on the target.
            val cachedPlaceholder = request.placeholderMemoryCacheKey?.let { memoryCache?.get(it)?.image }
            request.target?.onStart(placeholder = cachedPlaceholder ?: request.placeholder())
            eventListener.onStart(request)
            request.listener?.onStart(request)

            // Resolve the size.
            val sizeResolver = request.sizeResolver
            eventListener.resolveSizeStart(request, sizeResolver)
            val size = sizeResolver.size()
            eventListener.resolveSizeEnd(request, size)

            // Execute the interceptor chain.
            val result = withContext(request.interceptorCoroutineContext) {
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
                val result = ErrorResult(request, throwable)
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
        options.logger?.log(TAG, result.throwable) {
            "🚨 Failed - ${request.data}"
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
        val mainCoroutineContextLazy: Lazy<CoroutineContext>,
        val memoryCacheLazy: Lazy<MemoryCache?>,
        val diskCacheLazy: Lazy<DiskCache?>,
        val eventListenerFactory: EventListener.Factory,
        val componentRegistry: ComponentRegistry,
        val logger: Logger?,
    )
}

private fun CoroutineScope(logger: Logger?): CoroutineScope {
    val context = SupervisorJob() +
        CoroutineExceptionHandler { _, throwable -> logger?.log(TAG, throwable) }
    return CoroutineScope(context)
}

internal expect fun needsExecuteOnMainDispatcher(
    request: ImageRequest,
): Boolean

internal expect fun getDisposable(
    request: ImageRequest,
    job: Deferred<ImageResult>,
): Disposable

internal expect inline fun transition(
    result: ImageResult,
    target: Target?,
    eventListener: EventListener,
    setDrawable: () -> Unit,
)

@Suppress("UNCHECKED_CAST")
internal fun ComponentRegistry.Builder.addServiceLoaderComponents(
    options: RealImageLoader.Options,
): ComponentRegistry.Builder {
    if (options.serviceLoaderEnabled) {
        // Delay reading the fetchers and decoders until the fetching/decoding stage.
        addFetcherFactories {
            ServiceLoaderComponentRegistry.fetchers
                .sortedByDescending { it.priority() }
                .mapNotNullIndices { target ->
                    target as FetcherServiceLoaderTarget<Any>
                    val factory = target.factory() ?: return@mapNotNullIndices null
                    val type = target.type() ?: return@mapNotNullIndices null
                    factory to type
                }
        }
        addDecoderFactories {
            ServiceLoaderComponentRegistry.decoders
                .sortedByDescending { it.priority() }
                .mapNotNullIndices { target ->
                    target.factory()
                }
        }
    }
    return this
}

internal expect fun ComponentRegistry.Builder.addAndroidComponents(
    options: RealImageLoader.Options,
): ComponentRegistry.Builder

internal expect fun ComponentRegistry.Builder.addJvmComponents(
    options: RealImageLoader.Options,
): ComponentRegistry.Builder

internal expect fun ComponentRegistry.Builder.addAppleComponents(
    options: RealImageLoader.Options,
): ComponentRegistry.Builder

internal fun ComponentRegistry.Builder.addCommonComponents(): ComponentRegistry.Builder {
    return this
        // Mappers
        .add(StringMapper())
        .add(PathMapper())
        // Keyers
        .add(FileUriKeyer())
        .add(UriKeyer())
        // Fetchers
        .add(FileUriFetcher.Factory())
        .add(ByteArrayFetcher.Factory())
        .add(DataUriFetcher.Factory())
}

private const val TAG = "RealImageLoader"
private const val REQUEST_TYPE_ENQUEUE = 0
private const val REQUEST_TYPE_EXECUTE = 1
