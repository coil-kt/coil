package coil.fetch

import android.content.Context
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import coil.bitmappool.BitmapPool
import coil.size.PixelSize
import coil.util.createMockWebServer
import coil.util.createOptions
import coil.util.createTestMainDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class HttpFetcherTest {

    private lateinit var context: Context
    private lateinit var mainDispatcher: TestCoroutineDispatcher
    private lateinit var server: MockWebServer
    private lateinit var callFactory: Call.Factory
    private lateinit var pool: BitmapPool

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        mainDispatcher = createTestMainDispatcher()
        server = createMockWebServer(context, "normal.jpg")
        callFactory = OkHttpClient()
        pool = BitmapPool(0)
    }

    @After
    fun after() {
        Dispatchers.resetMain()
        server.shutdown()
    }

    @Test
    fun `basic network URL fetch`() {
        val fetcher = HttpUrlFetcher(callFactory)
        val url = server.url("/normal.jpg")
        assertTrue(fetcher.handles(url))
        assertEquals(url.toString(), fetcher.key(url))

        val result = runBlocking {
            fetcher.fetch(pool, url, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is SourceResult)
        assertFalse(result.source.exhausted())
    }

    @Test
    fun `basic network URI fetch`() {
        val fetcher = HttpUriFetcher(callFactory)
        val uri = server.url("/normal.jpg").toString().toUri()
        assertTrue(fetcher.handles(uri))
        assertEquals(uri.toString(), fetcher.key(uri))

        val result = runBlocking {
            fetcher.fetch(pool, uri, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is SourceResult)
        assertFalse(result.source.exhausted())
    }
}
