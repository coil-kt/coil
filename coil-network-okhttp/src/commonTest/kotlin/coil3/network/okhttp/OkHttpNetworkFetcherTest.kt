package coil3.network.okhttp

import coil3.network.InFlightRequestStrategy
import coil3.network.NetworkFetcher
import coil3.request.Options
import coil3.test.utils.AbstractNetworkFetcherTest
import coil3.toUri
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertIs
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.ByteString

class OkHttpNetworkFetcherTest : AbstractNetworkFetcherTest() {

    private lateinit var server: MockWebServer

    @BeforeTest
    override fun before() {
        super.before()
        server = MockWebServer().apply { start() }
    }

    @AfterTest
    override fun after() {
        super.after()
        server.shutdown()
    }

    override fun url(path: String): String {
        return server.url(path).toString()
    }

    override fun newFetcher(
        path: String,
        responseBody: ByteString,
        options: Options,
        inFlightRequestStrategy: InFlightRequestStrategy
    ): NetworkFetcher {
        server.enqueue(MockResponse().setBody(Buffer().apply { write(responseBody) }))
        val client = OkHttpClient()
        val factory = OkHttpNetworkFetcherFactory(
            callFactory = { client },
            inFlightRequestStrategy = { inFlightRequestStrategy },
        )
        return assertIs(factory.create(url(path).toUri(), options, imageLoader))
    }
}
