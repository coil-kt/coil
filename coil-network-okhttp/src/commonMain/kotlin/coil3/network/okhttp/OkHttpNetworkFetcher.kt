package coil3.network.okhttp

import coil3.network.CacheStrategy
import coil3.network.NetworkClient
import coil3.network.NetworkFetcher
import coil3.network.okhttp.internal.OkHttpNetworkClient
import okhttp3.OkHttpClient

@JvmOverloads
fun OkHttpNetworkFetcherFactory(
    httpClient: Lazy<OkHttpClient> = lazy { OkHttpClient() },
) = OkHttpNetworkFetcherFactory(
    httpClient = httpClient,
    cacheStrategy = lazy { CacheStrategy() },
)

fun OkHttpNetworkFetcherFactory(
    httpClient: Lazy<OkHttpClient>,
    cacheStrategy: Lazy<CacheStrategy>,
) = NetworkFetcher.Factory(
    networkClient = lazy { httpClient.value.asNetworkClient() },
    cacheStrategy = cacheStrategy,
)

fun OkHttpClient.asNetworkClient(): NetworkClient {
    return OkHttpNetworkClient(this)
}
