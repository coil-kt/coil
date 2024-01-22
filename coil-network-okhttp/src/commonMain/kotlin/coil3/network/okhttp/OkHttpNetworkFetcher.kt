@file:JvmName("OkHttpNetworkFetcher")

package coil3.network.okhttp

import coil3.network.CacheStrategy
import coil3.network.NetworkClient
import coil3.network.NetworkFetcher
import coil3.network.okhttp.internal.CallFactoryNetworkClient
import okhttp3.Call
import okhttp3.OkHttpClient

@JvmName("factory")
fun OkHttpNetworkFetcherFactory() = NetworkFetcher.Factory(
    networkClient = { OkHttpClient().asNetworkClient() },
    cacheStrategy = { CacheStrategy() },
)

@JvmName("factory")
fun OkHttpNetworkFetcherFactory(
    callFactory: () -> Call.Factory,
) = OkHttpNetworkFetcherFactory(
    callFactory = callFactory,
    cacheStrategy = { CacheStrategy() },
)

@JvmName("factory")
fun OkHttpNetworkFetcherFactory(
    callFactory: () -> Call.Factory,
    cacheStrategy: () -> CacheStrategy,
) = NetworkFetcher.Factory(
    networkClient = { callFactory().asNetworkClient() },
    cacheStrategy = cacheStrategy,
)

@JvmName("factory")
fun OkHttpNetworkFetcherFactory(
    callFactory: Call.Factory,
) = OkHttpNetworkFetcherFactory(
    callFactory = callFactory,
    cacheStrategy = CacheStrategy(),
)

@JvmName("factory")
fun OkHttpNetworkFetcherFactory(
    callFactory: Call.Factory,
    cacheStrategy: CacheStrategy,
) = NetworkFetcher.Factory(
    networkClient = { callFactory.asNetworkClient() },
    cacheStrategy = { cacheStrategy },
)

fun Call.Factory.asNetworkClient(): NetworkClient {
    return CallFactoryNetworkClient(this)
}
