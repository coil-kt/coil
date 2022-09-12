@file:JvmName("FakeImageLoaders")

package coil

import android.graphics.drawable.Drawable
import coil.FakeImageLoader.CompositeQueueEngine
import coil.FakeImageLoader.Engine
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.DefaultRequestOptions
import coil.request.Disposable
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.NullRequestData
import coil.request.NullRequestDataException
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

@ExperimentalCoilApi
class FakeImageLoader private constructor(
    coroutineContext: CoroutineContext,
    override val defaults: DefaultRequestOptions,
    override val components: ComponentRegistry,
    override val memoryCache: MemoryCache,
    override val diskCache: DiskCache,
    val engine: Engine,
) : ImageLoader {

    private val scope = CoroutineScope(coroutineContext)
    private val shutdown = AtomicBoolean(false)

    private val _requests = MutableSharedFlow<ImageRequest>()
    val requests: Flow<ImageRequest> get() = _requests

    private val _results = MutableSharedFlow<ImageResult>()
    val results: Flow<ImageResult> get() = _results

    override fun enqueue(request: ImageRequest): Disposable {
        startCommon(request)
        return JobDisposable(job = scope.async { executeCommon(request) })
    }

    override suspend fun execute(request: ImageRequest): ImageResult {
        startCommon(request)
        return executeCommon(request)
    }

    private fun startCommon(request: ImageRequest) {
        assertNotShutdown()
        assertValidRequest(request)
        _requests.tryEmit(request)
    }

    private suspend fun executeCommon(request: ImageRequest): ImageResult {
        request.target?.onStart(request.placeholder)
        val result = engine.execute(request)
        _results.tryEmit(result)
        return result
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

        private var coroutineContext = SupervisorJob() + Dispatchers.Unconfined
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
