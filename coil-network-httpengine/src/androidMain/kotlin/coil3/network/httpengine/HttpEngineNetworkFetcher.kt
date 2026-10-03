@file:JvmName("HttpEngineNetworkFetcher")

package coil3.network.httpengine

import android.net.http.HttpEngine
import androidx.annotation.RequiresApi
import coil3.PlatformContext
import coil3.Uri
import coil3.fetch.Fetcher
import coil3.network.CacheStrategy
import coil3.network.ConcurrentRequestStrategy
import coil3.network.ConnectivityChecker
import coil3.network.NetworkFetcher
import java.util.concurrent.Executor
import kotlin.jvm.JvmName
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
