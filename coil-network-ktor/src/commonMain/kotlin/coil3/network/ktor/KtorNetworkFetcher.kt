@file:JvmName("KtorNetworkFetcher")

package coil3.network.ktor

import coil3.network.CacheStrategy
import coil3.network.NetworkClient
import coil3.network.NetworkFetcher
import coil3.network.ktor.internal.KtorNetworkClient
import io.ktor.client.HttpClient
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

@JvmOverloads
@JvmName("factory")
fun KtorNetworkFetcherFactory(
    httpClient: Lazy<HttpClient> = lazy { HttpClient() },
) = KtorNetworkFetcherFactory(
    httpClient = httpClient,
    cacheStrategy = lazy { CacheStrategy() },
)

@JvmName("factory")
fun KtorNetworkFetcherFactory(
    httpClient: Lazy<HttpClient>,
    cacheStrategy: Lazy<CacheStrategy>,
) = NetworkFetcher.Factory(
    networkClient = lazy { httpClient.value.asNetworkClient() },
    cacheStrategy = cacheStrategy,
)

fun HttpClient.asNetworkClient(): NetworkClient {
    return KtorNetworkClient(this)
}
