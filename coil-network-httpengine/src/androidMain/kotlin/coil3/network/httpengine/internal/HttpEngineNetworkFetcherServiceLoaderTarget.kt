package coil3.network.httpengine.internal

import android.os.Build.VERSION.SDK_INT
import coil3.Uri
import coil3.fetch.Fetcher
import coil3.network.httpengine.HttpEngineNetworkFetcherFactory
import coil3.util.FetcherServiceLoaderTarget

internal class HttpEngineNetworkFetcherServiceLoaderTarget : FetcherServiceLoaderTarget<Uri> {
    override fun factory(): Fetcher.Factory<Uri>? {
        return if (SDK_INT >= 34) HttpEngineNetworkFetcherFactory() else null
    }
    override fun type() = Uri::class
    override fun priority() = -1 // okhttp > ktor3 > ktor2 > http-engine
}
