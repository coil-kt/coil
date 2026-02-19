@file:JvmName("KtorNetworkFetcher")

package coil3.network.ktor3

import coil3.PlatformContext
import coil3.network.CacheStrategy
import coil3.network.ConcurrentRequestStrategy
import coil3.network.ConnectivityChecker
import coil3.network.NetworkClient
import coil3.network.NetworkFetcher
import coil3.network.ktor3.internal.KtorNetworkClient
import io.ktor.client.HttpClient
import kotlin.jvm.JvmName

@JvmName("factory")
fun KtorNetworkFetcherFactory() = NetworkFetcher.Factory(
    networkClient = { HttpClient().asNetworkClient() },
)

@JvmName("factory")
@Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
fun KtorNetworkFetcherFactory(
    httpClient: HttpClient,
) = NetworkFetcher.Factory(
    networkClient = { httpClient.asNetworkClient() },
)

@JvmName("factory")
fun KtorNetworkFetcherFactory(
    httpClient: HttpClient,
    concurrentRequestStrategy: ConcurrentRequestStrategy = ConcurrentRequestStrategy.UNCOORDINATED,
) = NetworkFetcher.Factory(
    networkClient = { httpClient.asNetworkClient() },
    concurrentRequestStrategy = { concurrentRequestStrategy },
)

@JvmName("factory")
fun KtorNetworkFetcherFactory(
    httpClient: () -> HttpClient,
) = NetworkFetcher.Factory(
    networkClient = { httpClient().asNetworkClient() },
)

@JvmName("factory")
@Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
fun KtorNetworkFetcherFactory(
    httpClient: () -> HttpClient = { HttpClient() },
    cacheStrategy: () -> CacheStrategy = { CacheStrategy.DEFAULT },
    connectivityChecker: (PlatformContext) -> ConnectivityChecker = ::ConnectivityChecker,
) = NetworkFetcher.Factory(
    networkClient = { httpClient().asNetworkClient() },
    cacheStrategy = cacheStrategy,
    connectivityChecker = connectivityChecker,
)

@JvmName("factory")
fun KtorNetworkFetcherFactory(
    httpClient: () -> HttpClient = { HttpClient() },
    cacheStrategy: () -> CacheStrategy = { CacheStrategy.DEFAULT },
    connectivityChecker: (PlatformContext) -> ConnectivityChecker = ::ConnectivityChecker,
    concurrentRequestStrategy: () -> ConcurrentRequestStrategy = { ConcurrentRequestStrategy.UNCOORDINATED },
) = NetworkFetcher.Factory(
    networkClient = { httpClient().asNetworkClient() },
    cacheStrategy = cacheStrategy,
    connectivityChecker = connectivityChecker,
    concurrentRequestStrategy = concurrentRequestStrategy,
)

fun HttpClient.asNetworkClient(): NetworkClient {
    return KtorNetworkClient(this)
}
