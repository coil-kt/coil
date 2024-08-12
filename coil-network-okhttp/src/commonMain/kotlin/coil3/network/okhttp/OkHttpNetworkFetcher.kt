@file:JvmName("OkHttpNetworkFetcher")

package coil3.network.okhttp

import coil3.PlatformContext
import coil3.network.CacheStrategy
import coil3.network.ConnectivityChecker
import coil3.network.NetworkClient
import coil3.network.NetworkFetcher
import coil3.network.okhttp.internal.CallFactoryNetworkClient
import okhttp3.Call
import okhttp3.OkHttpClient

@JvmName("factory")
fun OkHttpNetworkFetcherFactory() = NetworkFetcher.Factory(
    networkClient = { OkHttpClient().asNetworkClient() },
)

@JvmName("factory")
fun OkHttpNetworkFetcherFactory(
    callFactory: Call.Factory,
) = NetworkFetcher.Factory(
    networkClient = { callFactory.asNetworkClient() },
)

@JvmName("factory")
fun OkHttpNetworkFetcherFactory(
    callFactory: () -> Call.Factory,
) = NetworkFetcher.Factory(
    networkClient = { callFactory().asNetworkClient() },
)

@JvmName("factory")
fun OkHttpNetworkFetcherFactory(
    callFactory: () -> Call.Factory = ::OkHttpClient,
    cacheStrategy: () -> CacheStrategy = { CacheStrategy.DEFAULT },
    connectivityChecker: (PlatformContext) -> ConnectivityChecker = ::ConnectivityChecker,
) = NetworkFetcher.Factory(
    networkClient = { callFactory().asNetworkClient() },
    cacheStrategy = cacheStrategy,
    connectivityChecker = connectivityChecker,
)

fun Call.Factory.asNetworkClient(): NetworkClient {
    return CallFactoryNetworkClient(this)
}
