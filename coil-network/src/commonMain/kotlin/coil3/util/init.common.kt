package coil3.util

import coil3.Uri
import coil3.annotation.InternalCoilApi
import coil3.fetch.NetworkFetcher

@InternalCoilApi
internal class NetworkFetcherServiceLoaderTarget : FetcherServiceLoaderTarget<Uri> {
    override fun factory() = NetworkFetcher.Factory()
    override fun type() = Uri::class
}
