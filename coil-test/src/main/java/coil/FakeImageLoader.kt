@file:JvmName("FakeImageLoaders")

package coil

import android.graphics.drawable.Drawable
import coil.FakeImageLoader.Engine
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.disk.DiskCache
import coil.disk.FakeDiskCache
import coil.memory.FakeMemoryCache
import coil.memory.MemoryCache
import coil.request.DefaultRequestOptions
import coil.request.Disposable
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.NullRequestData
import coil.request.NullRequestDataException
import coil.request.SuccessResult
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.merge

@ExperimentalCoilApi
class FakeImageLoader private constructor(
    coroutineContext: CoroutineContext,
    override val defaults: DefaultRequestOptions,
    override val components: ComponentRegistry,
    override val memoryCache: MemoryCache,
    override val diskCache: DiskCache,
    val engine: Engine,
) : ImageLoader {

    private val scope = CoroutineScope(SupervisorJob() + coroutineContext)
    private val shutdown = AtomicBoolean(false)
    private val _enqueues = MutableSharedFlow<ImageRequest>()
    private val _executes = MutableSharedFlow<ImageRequest>()
    private val _results = MutableSharedFlow<Pair<ImageRequest, ImageResult>>()

    /** Returns a [Flow] that emits when [enqueue] is called. */
    val enqueues: Flow<ImageRequest> = _enqueues.asSharedFlow()

    /** Returns a [Flow] that emits when [execute] is called. */
    val executes: Flow<ImageRequest> = _executes.asSharedFlow()

    /** Returns a [Flow] that emits when either [enqueue] or [execute] is called. */
    val requests: Flow<ImageRequest> = merge(enqueues, executes)

    /** Returns a [Flow] that emits when a request completes. */
    val results: Flow<Pair<ImageRequest, ImageResult>> = _results.asSharedFlow()

    override fun enqueue(request: ImageRequest): Disposable {
        _enqueues.tryEmit(request)
        startCommon(request)
        return JobDisposable(job = scope.async { executeCommon(request) })
    }

    override suspend fun execute(request: ImageRequest): ImageResult {
        _executes.tryEmit(request)
        startCommon(request)
        return executeCommon(request)
    }

    private fun startCommon(request: ImageRequest) {
        assertNotShutdown()
        assertValidRequest(request)
    }

    private suspend fun executeCommon(request: ImageRequest): ImageResult {
        // Always call onStart before executing the request.
        request.target?.onStart(request.placeholder)
        return engine.execute(request).also { result ->
            _results.tryEmit(request to result)
        }
    }

    override fun shutdown() {
        shutdown.set(true)
    }

    override fun newBuilder(): ImageLoader.Builder {
        throw UnsupportedOperationException("newBuilder() is not supported")
    }

    fun assertNotShutdown() {
        check(!shutdown.get()) { "image loader is shutdown" }
    }

    private fun assertValidRequest(request: ImageRequest) {
        if (request.data === NullRequestData) {
            throw NullRequestDataException()
        }
    }

    fun interface Engine {
        suspend fun execute(request: ImageRequest): ImageResult
    }

    private class JobDisposable(
        override val job: Deferred<ImageResult>
    ) : Disposable {

        override val isDisposed: Boolean
            get() = !job.isActive

        override fun dispose() {
            if (isDisposed) return
            job.cancel()
        }
    }

    class Builder {

        private var coroutineContext: CoroutineContext = Dispatchers.Unconfined
        private var defaults = DefaultRequestOptions()
        private var components = ComponentRegistry()
        private var memoryCache: MemoryCache = FakeMemoryCache()
        private var diskCache: DiskCache = FakeDiskCache()
        private var engine: Engine = CompositeQueueEngine()

        fun coroutineContext(context: CoroutineContext) = apply {
            this.coroutineContext = context
        }

        fun defaults(defaults: DefaultRequestOptions) = apply {
            this.defaults = defaults
        }

        fun components(components: ComponentRegistry) = apply {
            this.components = components
        }

        fun memoryCache(memoryCache: MemoryCache) = apply {
            this.memoryCache = memoryCache
        }

        fun diskCache(diskCache: DiskCache) = apply {
            this.diskCache = diskCache
        }

        fun engine(engine: Engine) = apply {
            this.engine = engine
        }

        fun build() = FakeImageLoader(
            coroutineContext = coroutineContext,
            defaults = defaults,
            components = components,
            memoryCache = memoryCache,
            diskCache = diskCache,
            engine = engine,
        )
    }
}

/**
 * Create a new [FakeImageLoader] without configuration.
 */
@JvmName("create")
fun FakeImageLoader(): FakeImageLoader {
    return FakeImageLoader.Builder().build()
}

/** Convenience function to get [FakeImageLoader.memoryCache] as a [FakeMemoryCache]. */
val FakeImageLoader.fakeMemoryCache: FakeMemoryCache get() = memoryCache as FakeMemoryCache

/** Convenience function to get [FakeImageLoader.diskCache] as a [FakeDiskCache]. */
val FakeImageLoader.fakeDiskCache: FakeDiskCache get() = diskCache as FakeDiskCache

fun FakeImageLoader.enqueue(
    data: Any,
    drawable: Drawable,
    dataSource: DataSource = DataSource.DISK,
    memoryCacheKey: MemoryCache.Key? = null,
    diskCacheKey: String? = null,
) = (engine as CompositeQueueEngine).enqueue(
    data = data,
    drawable = drawable,
    dataSource = dataSource,
    memoryCacheKey = memoryCacheKey,
    diskCacheKey = diskCacheKey
)

fun FakeImageLoader.enqueue(
    data: Any,
    engine: Engine
) = (engine as CompositeQueueEngine).enqueue(
    data = data,
    engine = engine,
)

class CompositeQueueEngine : Engine {

    private val results = mutableMapOf<Any, Queue<Engine>>()
    private var default: Engine? = null

    @Synchronized
    fun enqueue(
        data: Any,
        drawable: Drawable,
        dataSource: DataSource = DataSource.DISK,
        memoryCacheKey: MemoryCache.Key? = null,
        diskCacheKey: String? = null,
    ) = enqueue(
        data = data,
        engine = { request ->
            SuccessResult(
                drawable = drawable,
                request = request,
                dataSource = dataSource,
                memoryCacheKey = memoryCacheKey,
                diskCacheKey = diskCacheKey
            )
        }
    )

    @Synchronized
    fun enqueue(data: Any, engine: Engine) = apply {
        results.getOrPut(data) { LinkedList() }.offer(engine)
    }

    @Synchronized
    fun default(default: Engine) = apply {
        this.default = default
    }

    override suspend fun execute(request: ImageRequest): ImageResult {
        // NOTE: synchronized works here because we don't suspend inside the block.
        val engine = synchronized(this) {
            checkNotNull(results[request.data]?.poll() ?: default) {
                "no queued result for $request. results: $results"
            }
        }
        return engine.execute(request)
    }
}
