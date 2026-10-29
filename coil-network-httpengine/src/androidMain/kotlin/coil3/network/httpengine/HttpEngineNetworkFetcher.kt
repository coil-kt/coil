@file:JvmName("HttpEngineNetworkFetcher")

package coil3.network.httpengine

import android.net.http.HttpEngine
import androidx.annotation.RequiresApi
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.Uri
import coil3.fetch.Fetcher
import coil3.network.CacheStrategy
import coil3.network.ConcurrentRequestStrategy
import coil3.network.ConnectivityChecker
import coil3.network.NetworkFetcher
import coil3.network.internal.singleParameterLazy
import coil3.request.Options
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

@JvmName("factory")
@RequiresApi(34)
fun HttpEngineNetworkFetcherFactory(
    httpEngine: HttpEngine,
    executor: Executor = Dispatchers.IO.asExecutor(),
    cacheStrategy: () -> CacheStrategy = { CacheStrategy.DEFAULT },
    connectivityChecker: (PlatformContext) -> ConnectivityChecker = ::ConnectivityChecker,
    concurrentRequestStrategy: () -> ConcurrentRequestStrategy = {
        ConcurrentRequestStrategy.UNCOORDINATED
    },
): Fetcher.Factory<Uri> {
    return NetworkFetcher.Factory(
        networkClient = { HttpEngineNetworkClient(httpEngine, executor) },
        cacheStrategy = cacheStrategy,
        connectivityChecker = connectivityChecker,
        concurrentRequestStrategy = concurrentRequestStrategy,
    )
}

@RequiresApi(34)
internal class HttpEngineFetcherFactory : Fetcher.Factory<Uri> {
    private val delegate = singleParameterLazy<PlatformContext, NetworkFetcher.Factory> { context ->
        NetworkFetcher.Factory(
            networkClient = {
                HttpEngineNetworkClient(
                    httpEngine = HttpEngine.Builder(context).build(),
                    executor = Dispatchers.IO.asExecutor(),
                )
            },
        )
    }

    override fun create(
        data: Uri,
        options: Options,
        imageLoader: ImageLoader,
    ): Fetcher? {
        if (data.scheme != "http" && data.scheme != "https") return null

        return delegate.get(options.context.applicationContext).create(data, options, imageLoader)
    }
}
