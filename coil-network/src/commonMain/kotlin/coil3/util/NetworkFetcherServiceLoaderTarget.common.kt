package coil3.util

import coil3.Uri
import coil3.fetch.NetworkFetcher

internal class NetworkFetcherServiceLoaderTarget : FetcherServiceLoaderTarget<Uri> {
    override fun factory() = NetworkFetcher.Factory()
    override fun type() = Uri::class
}
