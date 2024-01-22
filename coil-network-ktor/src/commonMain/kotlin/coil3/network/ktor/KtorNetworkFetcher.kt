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
    networkClient = lazy { HttpClient().asNetworkClient() },
    cacheStrategy = lazy { CacheStrategy() },
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
    networkClient = lazy { httpClient().asNetworkClient() },
    cacheStrategy = lazy(cacheStrategy),
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
    networkClient = lazyOf(httpClient.asNetworkClient()),
    cacheStrategy = lazyOf(cacheStrategy),
)

fun HttpClient.asNetworkClient(): NetworkClient {
    return KtorNetworkClient(this)
}
