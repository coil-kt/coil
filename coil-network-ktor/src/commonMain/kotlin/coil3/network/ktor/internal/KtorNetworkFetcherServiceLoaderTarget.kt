package coil3.network.ktor.internal

import coil3.Uri
import coil3.network.ktor.KtorNetworkFetcherFactory
import coil3.util.FetcherServiceLoaderTarget

internal class KtorNetworkFetcherServiceLoaderTarget : FetcherServiceLoaderTarget<Uri> {
    override fun factory() = KtorNetworkFetcherFactory()
    override fun type() = Uri::class
}
