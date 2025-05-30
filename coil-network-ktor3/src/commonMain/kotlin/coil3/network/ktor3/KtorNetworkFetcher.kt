@file:JvmName("KtorNetworkFetcher")

package coil3.network.ktor3

import coil3.PlatformContext
import coil3.network.CacheStrategy
import coil3.network.ConnectivityChecker
import coil3.network.InFlightRequestStrategy
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
    inFlightRequestStrategy: InFlightRequestStrategy = InFlightRequestStrategy.DEFAULT
) = NetworkFetcher.Factory(
    networkClient = { httpClient.asNetworkClient() },
    inFlightRequestStrategy = { inFlightRequestStrategy }
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
    inFlightRequestStrategy: () -> InFlightRequestStrategy = { InFlightRequestStrategy.DEFAULT }
) = NetworkFetcher.Factory(
    networkClient = { httpClient().asNetworkClient() },
    cacheStrategy = cacheStrategy,
    connectivityChecker = connectivityChecker,
    inFlightRequestStrategy = inFlightRequestStrategy
)

fun HttpClient.asNetworkClient(): NetworkClient {
    return KtorNetworkClient(this)
}
