@file:JvmName("KtorNetworkFetcher")

package coil3.network.ktor2

import coil3.PlatformContext
import coil3.network.CacheStrategy
import coil3.network.ConnectivityChecker
import coil3.network.NetworkClient
import coil3.network.NetworkFetcher
import coil3.network.ktor2.internal.KtorNetworkClient
import io.ktor.client.HttpClient
import kotlin.jvm.JvmName

@JvmName("factory")
fun KtorNetworkFetcherFactory() = NetworkFetcher.Factory(
    networkClient = { HttpClient().asNetworkClient() },
)

@JvmName("factory")
fun KtorNetworkFetcherFactory(
    httpClient: HttpClient,
) = NetworkFetcher.Factory(
    networkClient = { httpClient.asNetworkClient() },
)

@JvmName("factory")
fun KtorNetworkFetcherFactory(
    httpClient: () -> HttpClient,
) = NetworkFetcher.Factory(
    networkClient = { httpClient().asNetworkClient() },
)

@JvmName("factory")
fun KtorNetworkFetcherFactory(
    httpClient: () -> HttpClient = { HttpClient() },
    cacheStrategy: () -> CacheStrategy = { CacheStrategy.DEFAULT },
    connectivityChecker: (PlatformContext) -> ConnectivityChecker = ::ConnectivityChecker,
) = NetworkFetcher.Factory(
    networkClient = { httpClient().asNetworkClient() },
    cacheStrategy = cacheStrategy,
    connectivityChecker = connectivityChecker,
)

fun HttpClient.asNetworkClient(): NetworkClient {
    return KtorNetworkClient(this)
}
