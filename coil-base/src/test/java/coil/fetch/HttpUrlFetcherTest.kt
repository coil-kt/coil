package coil.fetch

import android.content.Context
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
@UseExperimental(ExperimentalCoroutinesApi::class)
class HttpUrlFetcherTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var mainDispatcher: TestCoroutineDispatcher
    private lateinit var server: MockWebServer
    private lateinit var fetcher: HttpUrlFetcher
    private lateinit var pool: BitmapPool

    @Before
    fun before() {
        mainDispatcher = createTestMainDispatcher()
        server = createMockWebServer(context, "normal.jpg")
        fetcher = HttpUrlFetcher(OkHttpClient())
        pool = BitmapPool(0)
    }

    @After
    fun after() {
        Dispatchers.resetMain()
        server.shutdown()
    }

    @Test
    fun `basic network fetch`() {
        val url = server.url("/normal.jpg")
        assertTrue(fetcher.handles(url))
        assertEquals(url.toString(), fetcher.key(url))

        val result = runBlocking {
            fetcher.fetch(pool, url, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is SourceResult)
        assertFalse(result.source.exhausted())
    }
}
