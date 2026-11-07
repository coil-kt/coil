package coil3.network.httpengine

import android.os.Build
import coil3.Uri
import coil3.fetch.Fetcher
import coil3.util.FetcherServiceLoaderTarget

internal class HttpEngineNetworkFetcherServiceLoaderTarget : FetcherServiceLoaderTarget<Uri> {
    override fun factory(): Fetcher.Factory<Uri>? {
        if (Build.VERSION.SDK_INT < 34) return null
        return HttpEngineFetcherFactory()
    }
    override fun type() = Uri::class
    override fun priority() = -1 // http-engine > okhttp > ktor3 > ktor2
}
