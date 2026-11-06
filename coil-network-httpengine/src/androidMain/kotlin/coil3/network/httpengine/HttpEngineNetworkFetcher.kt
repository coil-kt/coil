@file:JvmName("HttpEngineNetworkFetcher")

package coil3.network.httpengine

import android.content.Context
import android.net.http.HttpEngine
import androidx.annotation.RequiresApi
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.Uri
import coil3.fetch.Fetcher
import coil3.network.CacheStrategy
import coil3.network.ConcurrentRequestStrategy
import coil3.network.ConnectivityChecker
import coil3.network.NetworkClient
import coil3.network.NetworkFetcher
import coil3.network.httpengine.internal.HttpEngineNetworkClient
import coil3.network.internal.singleParameterLazy
import coil3.request.Options
import java.util.concurrent.Executor
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

/** Creates a fetcher backed by the shared, process-wide [HttpEngine]. */
@JvmName("factory")
@RequiresApi(34)
@OptIn(ExperimentalTime::class)
fun HttpEngineNetworkFetcherFactory(
    executor: Executor = Dispatchers.IO.asExecutor(),
    clock: Clock = Clock.System,
    redirectPolicy: HttpEngineRedirectPolicy = HttpEngineRedirectPolicy.DEFAULT,
    cacheStrategy: () -> CacheStrategy = { CacheStrategy.DEFAULT },
    connectivityChecker: (PlatformContext) -> ConnectivityChecker = ::ConnectivityChecker,
    concurrentRequestStrategy: () -> ConcurrentRequestStrategy = {
        ConcurrentRequestStrategy.UNCOORDINATED
    },
): Fetcher.Factory<Uri> {
    return HttpEngineFetcherFactory(
        executor = executor,
        clock = clock,
        redirectPolicy = redirectPolicy,
        cacheStrategy = cacheStrategy,
        connectivityChecker = connectivityChecker,
        concurrentRequestStrategy = concurrentRequestStrategy,
    )
}

@JvmName("factory")
@RequiresApi(34)
@OptIn(ExperimentalTime::class)
fun HttpEngineNetworkFetcherFactory(
    httpEngine: HttpEngine,
    executor: Executor = Dispatchers.IO.asExecutor(),
    clock: Clock = Clock.System,
    redirectPolicy: HttpEngineRedirectPolicy = HttpEngineRedirectPolicy.DEFAULT,
    cacheStrategy: () -> CacheStrategy = { CacheStrategy.DEFAULT },
    connectivityChecker: (PlatformContext) -> ConnectivityChecker = ::ConnectivityChecker,
    concurrentRequestStrategy: () -> ConcurrentRequestStrategy = {
        ConcurrentRequestStrategy.UNCOORDINATED
    },
): Fetcher.Factory<Uri> {
    return NetworkFetcher.Factory(
        networkClient = { HttpEngineNetworkClient(httpEngine, executor, clock, redirectPolicy) },
        cacheStrategy = cacheStrategy,
        connectivityChecker = connectivityChecker,
        concurrentRequestStrategy = concurrentRequestStrategy,
    )
}

@RequiresApi(34)
@OptIn(ExperimentalTime::class)
fun HttpEngine.asNetworkClient(
    executor: Executor = Dispatchers.IO.asExecutor(),
    clock: Clock = Clock.System,
    redirectPolicy: HttpEngineRedirectPolicy = HttpEngineRedirectPolicy.DEFAULT,
): NetworkClient {
    return HttpEngineNetworkClient(this, executor, clock, redirectPolicy)
}

@RequiresApi(34)
@OptIn(ExperimentalTime::class)
private class HttpEngineFetcherFactory(
    executor: Executor = Dispatchers.IO.asExecutor(),
    clock: Clock = Clock.System,
    redirectPolicy: HttpEngineRedirectPolicy = HttpEngineRedirectPolicy.DEFAULT,
    cacheStrategy: () -> CacheStrategy = { CacheStrategy.DEFAULT },
    connectivityChecker: (PlatformContext) -> ConnectivityChecker = ::ConnectivityChecker,
    concurrentRequestStrategy: () -> ConcurrentRequestStrategy = {
        ConcurrentRequestStrategy.UNCOORDINATED
    },
) : Fetcher.Factory<Uri> {
    private val delegate = singleParameterLazy { context: Context ->
        NetworkFetcher.Factory(
            networkClient = {
                HttpEngineNetworkClient(
                    httpEngine = sharedHttpEngine.get(context),
                    executor = executor,
                    clock = clock,
                    redirectPolicy = redirectPolicy,
                )
            },
            cacheStrategy = cacheStrategy,
            connectivityChecker = connectivityChecker,
            concurrentRequestStrategy = concurrentRequestStrategy,
        )
    }

    override fun create(
        data: Uri,
        options: Options,
        imageLoader: ImageLoader,
    ): Fetcher? {
        if (!isApplicable(data)) return null
        return delegate.get(options.context.applicationContext).create(data, options, imageLoader)
    }

    private fun isApplicable(data: Uri): Boolean {
        return data.scheme == "http" || data.scheme == "https"
    }

    private companion object {
        private val sharedHttpEngine = singleParameterLazy { context: Context ->
            HttpEngine.Builder(context).build()
        }
    }
}
