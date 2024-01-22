@file:JvmName("KtorNetworkFetcher")

package coil3.network.ktor

import coil3.network.CacheStrategy
import coil3.network.NetworkClient
import coil3.network.NetworkFetcher
import coil3.network.ktor.internal.KtorNetworkClient
import io.ktor.client.HttpClient
import kotlin.jvm.JvmName

@JvmName("factory")
fun KtorNetworkFetcherFactory() = NetworkFetcher.Factory(
    networkClient = { HttpClient().asNetworkClient() },
    cacheStrategy = { CacheStrategy() },
)

@JvmName("factory")
fun KtorNetworkFetcherFactory(
    httpClient: () -> HttpClient,
) = KtorNetworkFetcherFactory(
    httpClient = httpClient,
    cacheStrategy = { CacheStrategy() },
)

@JvmName("factory")
fun KtorNetworkFetcherFactory(
    httpClient: () -> HttpClient,
    cacheStrategy: () -> CacheStrategy,
) = NetworkFetcher.Factory(
    networkClient = { httpClient().asNetworkClient() },
    cacheStrategy = cacheStrategy,
)

@JvmName("factory")
fun KtorNetworkFetcherFactory(
    httpClient: HttpClient,
) = KtorNetworkFetcherFactory(
    httpClient = httpClient,
    cacheStrategy = CacheStrategy(),
)

@JvmName("factory")
fun KtorNetworkFetcherFactory(
    httpClient: HttpClient,
    cacheStrategy: CacheStrategy,
) = NetworkFetcher.Factory(
    networkClient = { httpClient.asNetworkClient() },
    cacheStrategy = { cacheStrategy },
)

fun HttpClient.asNetworkClient(): NetworkClient {
    return KtorNetworkClient(this)
}
