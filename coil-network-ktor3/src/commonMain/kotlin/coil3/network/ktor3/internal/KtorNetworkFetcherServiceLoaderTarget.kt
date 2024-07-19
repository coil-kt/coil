package coil3.network.ktor3.internal

import coil3.Uri
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.util.FetcherServiceLoaderTarget

internal class KtorNetworkFetcherServiceLoaderTarget : FetcherServiceLoaderTarget<Uri> {
    override fun factory() = KtorNetworkFetcherFactory()
    override fun type() = Uri::class
    override fun priority() = 1 // okhttp > ktor3 > ktor2
}
