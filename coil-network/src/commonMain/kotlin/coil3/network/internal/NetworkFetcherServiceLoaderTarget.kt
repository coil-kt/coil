package coil3.network.internal

import coil3.Uri
import coil3.network.NetworkFetcher
import coil3.util.FetcherServiceLoaderTarget

internal class NetworkFetcherServiceLoaderTarget : FetcherServiceLoaderTarget<Uri> {
    override fun factory() = NetworkFetcher.Factory()
    override fun type() = Uri::class
}
