package coil.fetch

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import coil.bitmap.BitmapPool
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
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
            fetcher.fetch(pool, url, PixelSize(100, 100), createOptions(context))
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
            fetcher.fetch(pool, uri, PixelSize(100, 100), createOptions(context))
        }

        assertTrue(result is SourceResult)
        assertFalse(result.source.exhausted())
    }

    @Test
    fun `mime type is parsed correctly from content type`() {
        val fetcher = HttpUriFetcher(callFactory)

        // https://android.googlesource.com/platform/frameworks/base/+/61ae88e/core/java/android/webkit/MimeTypeMap.java#407
        Shadows.shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypMapping("svg", "image/svg+xml")

        val url1 = HttpUrl.get("https://www.example.com/image.jpg")
        val body1 = ResponseBody.create(MediaType.get("image/svg+xml"), byteArrayOf())
        assertEquals("image/svg+xml", fetcher.getMimeType(url1, body1))

        val url2 = HttpUrl.get("https://www.example.com/image.svg")
        val body2 = ResponseBody.create(null, byteArrayOf())
        assertEquals("image/svg+xml", fetcher.getMimeType(url2, body2))

        val url3 = HttpUrl.get("https://www.example.com/image")
        val body3 = ResponseBody.create(MediaType.get("image/svg+xml;charset=utf-8"), byteArrayOf())
        assertEquals("image/svg+xml", fetcher.getMimeType(url3, body3))

        val url4 = HttpUrl.get("https://www.example.com/image.svg")
        val body4 = ResponseBody.create(MediaType.get("text/plain"), byteArrayOf())
        assertEquals("image/svg+xml", fetcher.getMimeType(url4, body4))

        val url5 = HttpUrl.get("https://www.example.com/image")
        val body5 = ResponseBody.create(null, byteArrayOf())
        assertNull(fetcher.getMimeType(url5, body5))
    }
}
