package coil3.network.httpengine

import android.net.http.HttpEngine
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import coil3.network.ConcurrentRequestStrategy
import coil3.network.NetworkFetcher
import coil3.request.Options
import coil3.test.utils.AbstractNetworkFetcherTest
import coil3.test.utils.context
import coil3.toUri
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.ByteString
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 34)
@OptIn(ExperimentalTime::class)
class HttpEngineNetworkFetcherTest : AbstractNetworkFetcherTest() {

    private lateinit var server: MockWebServer
    private lateinit var engine: HttpEngine

    @BeforeTest
    override fun before() {
        super.before()
        server = MockWebServer().apply { start() }
        engine = HttpEngine.Builder(context).build()
    }

    @AfterTest
    override fun after() {
        try {
            super.after()
        } finally {
            try {
                engine.shutdown()
            } finally {
                server.shutdown()
            }
        }
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
        server.enqueue(MockResponse().setBody(Buffer().write(responseBody)))
        val factory = HttpEngineNetworkFetcherFactory(
            httpEngine = engine,
            concurrentRequestStrategy = { concurrentRequestStrategy },
        )
        return assertIs(factory.create(url(path).toUri(), options, imageLoader))
    }
}
