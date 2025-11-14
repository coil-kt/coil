package coil3.network.ktor2

import coil3.network.InFlightRequestStrategy
import coil3.network.NetworkFetcher
import coil3.request.Options
import coil3.test.utils.AbstractNetworkFetcherTest
import coil3.toUri
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.assertIs
import okio.ByteString

class KtorNetworkFetcherTest : AbstractNetworkFetcherTest() {

    override fun url(path: String): String {
        return "https://example.com/$path"
    }

    override fun newFetcher(
        path: String,
        responseBody: ByteString,
        options: Options,
        inFlightRequestStrategy: InFlightRequestStrategy
    ): NetworkFetcher {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(responseBody.toByteArray())
                }
            }
        }
        val factory = KtorNetworkFetcherFactory(client, inFlightRequestStrategy)
        return assertIs(factory.create(url(path).toUri(), options, imageLoader))
    }
}
