package coil3.network.okhttp

import coil3.fetch.SourceFetchResult
import coil3.network.ConcurrentRequestStrategy
import coil3.network.NetworkFetcher
import coil3.request.Options
import coil3.test.utils.AbstractNetworkFetcherTest
import coil3.test.utils.context
import coil3.test.utils.runTestAsync
import coil3.toUri
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
        concurrentRequestStrategy: ConcurrentRequestStrategy,
    ): NetworkFetcher {
        server.enqueue(MockResponse().setBody(Buffer().apply { write(responseBody) }))
        val client = OkHttpClient()
        val factory = OkHttpNetworkFetcherFactory(
            callFactory = { client },
            concurrentRequestStrategy = { concurrentRequestStrategy },
        )
        return assertIs(factory.create(url(path).toUri(), options, imageLoader))
    }

    @Test
    fun interceptorHeadersAreSent() = runTestAsync {
        server.enqueue(MockResponse().setBody(Buffer().apply { write(ByteString.EMPTY) }))
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "coil-test")
                    .header("Accept", "image/png")
                    .build()
                chain.proceed(request)
            }
            .build()
        val factory = OkHttpNetworkFetcherFactory(
            callFactory = { client },
            concurrentRequestStrategy = { ConcurrentRequestStrategy.UNCOORDINATED },
        )
        val fetcher = assertIs<NetworkFetcher>(
            factory.create(url("image.png").toUri(), Options(context), imageLoader),
        )

        val result = assertIs<SourceFetchResult>(fetcher.fetch())
        result.source.close()

        val request = server.takeRequest()
        assertEquals("coil-test", request.headers["User-Agent"])
        assertEquals("image/png", request.headers["Accept"])
    }
}
