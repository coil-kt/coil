@file:JvmName("OkHttpNetworkFetcher")

package coil3.network.okhttp

import coil3.PlatformContext
import coil3.network.CacheStrategy
import coil3.network.ConnectivityChecker
import coil3.network.InFlightRequestStrategy
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
) = NetworkFetcher.Factory(
    networkClient = { callFactory().asNetworkClient() },
    cacheStrategy = cacheStrategy,
)

@JvmName("factory")
@Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
fun OkHttpNetworkFetcherFactory(
    callFactory: () -> Call.Factory = ::OkHttpClient,
    cacheStrategy: () -> CacheStrategy = { CacheStrategy.DEFAULT },
    connectivityChecker: (PlatformContext) -> ConnectivityChecker = ::ConnectivityChecker,
) = NetworkFetcher.Factory(
    networkClient = { callFactory().asNetworkClient() },
    cacheStrategy = cacheStrategy,
    connectivityChecker = connectivityChecker,
)

@JvmName("factory")
fun OkHttpNetworkFetcherFactory(
    callFactory: () -> Call.Factory = ::OkHttpClient,
    cacheStrategy: () -> CacheStrategy = { CacheStrategy.DEFAULT },
    inFlightRequestStrategy: () -> InFlightRequestStrategy = { InFlightRequestStrategy.DEFAULT },
    connectivityChecker: (PlatformContext) -> ConnectivityChecker = ::ConnectivityChecker,
) = NetworkFetcher.Factory(
    networkClient = { callFactory().asNetworkClient() },
    cacheStrategy = cacheStrategy,
    connectivityChecker = connectivityChecker,
    inFlightRequestStrategy = inFlightRequestStrategy,
)

fun Call.Factory.asNetworkClient(): NetworkClient {
    return CallFactoryNetworkClient(this)
}
