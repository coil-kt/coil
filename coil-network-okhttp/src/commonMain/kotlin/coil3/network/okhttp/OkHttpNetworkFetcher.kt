@file:JvmName("OkHttpNetworkFetcher")

package coil3.network.okhttp

import coil3.network.CacheStrategy
import coil3.network.NetworkClient
import coil3.network.NetworkFetcher
import coil3.network.okhttp.internal.CallFactoryNetworkClient
import okhttp3.Call
import okhttp3.OkHttpClient

@JvmOverloads
@JvmName("factory")
fun OkHttpNetworkFetcherFactory(
    callFactory: Lazy<Call.Factory> = lazy { OkHttpClient() },
) = OkHttpNetworkFetcherFactory(
    callFactory = callFactory,
    cacheStrategy = lazy { CacheStrategy() },
)

@JvmName("factory")
fun OkHttpNetworkFetcherFactory(
    callFactory: Lazy<Call.Factory>,
    cacheStrategy: Lazy<CacheStrategy>,
) = NetworkFetcher.Factory(
    networkClient = lazy { callFactory.value.asNetworkClient() },
    cacheStrategy = cacheStrategy,
)

fun Call.Factory.asNetworkClient(): NetworkClient {
    return CallFactoryNetworkClient(this)
}
