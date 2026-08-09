package coil3.fetch.internal

import coil3.Uri
import coil3.fetch.BlobUriFetcher
import coil3.util.FetcherServiceLoaderTarget

internal class BlobUriFetcherServiceLoaderTarget : FetcherServiceLoaderTarget<Uri> {
    override fun factory() = BlobUriFetcher.Factory()
    override fun type() = Uri::class
}
