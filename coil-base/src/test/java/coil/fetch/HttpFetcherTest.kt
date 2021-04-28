package coil.fetch

import android.content.Context
import android.os.NetworkOnMainThreadException
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import coil.bitmap.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.network.HttpException
import coil.request.CachePolicy
import coil.size.PixelSize
import coil.util.createMockWebServer
import coil.util.createTestMainDispatcher
import coil.util.runBlockingTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import okhttp3.Cache
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class HttpFetcherTest {

    private lateinit var cache: Cache
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
        cache = Cache(File("build/cache"), Long.MAX_VALUE).apply { evictAll() }
        callFactory = OkHttpClient().newBuilder().cache(cache).build()
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
            fetcher.fetch(pool, url, PixelSize(100, 100), Options(context))
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
            fetcher.fetch(pool, uri, PixelSize(100, 100), Options(context))
        }

        assertTrue(result is SourceResult)
        assertFalse(result.source.exhausted())
    }

    @Test
    fun `mime type is parsed correctly from content type`() {
        val fetcher = HttpUriFetcher(callFactory)

        // https://android.googlesource.com/platform/frameworks/base/+/61ae88e/core/java/android/webkit/MimeTypeMap.java#407
        Shadows.shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypMapping("svg", "image/svg+xml")

        val url1 = "https://www.example.com/image.jpg".toHttpUrl()
        val body1 = "".toResponseBody("image/svg+xml".toMediaType())
        assertEquals("image/svg+xml", fetcher.getMimeType(url1, body1))

        val url2 = "https://www.example.com/image.svg".toHttpUrl()
        val body2 = "".toResponseBody(null)
        assertEquals("image/svg+xml", fetcher.getMimeType(url2, body2))

        val url3 = "https://www.example.com/image".toHttpUrl()
        val body3 = "".toResponseBody("image/svg+xml;charset=utf-8".toMediaType())
        assertEquals("image/svg+xml", fetcher.getMimeType(url3, body3))

        val url4 = "https://www.example.com/image.svg".toHttpUrl()
        val body4 = "".toResponseBody("text/plain".toMediaType())
        assertEquals("image/svg+xml", fetcher.getMimeType(url4, body4))

        val url5 = "https://www.example.com/image".toHttpUrl()
        val body5 = "".toResponseBody(null)
        assertNull(fetcher.getMimeType(url5, body5))
    }

    @Test
    fun `not found response is cached`() {
        val uri = createMockWebServer(context).url("/notfound.jpg").toString().toUri()
        val fetcher = HttpUriFetcher(callFactory)

        assertFailsWith<HttpException> {
            runBlocking {
                val result = fetcher.fetch(pool, uri, PixelSize(100, 100), Options(context))
                (result as SourceResult).source.close()
            }
        }
        assertEquals(uri.toString(), cache.urls().next())
    }

    @Test
    fun `request on main thread throws NetworkOnMainThreadException`() = runBlockingTest {
        val fetcher = HttpUriFetcher(callFactory)
        val uri = server.url("/normal.jpg").toString().toUri()
        val options = Options(context)

        assertFailsWith<NetworkOnMainThreadException> {
            fetcher.fetch(pool, uri, PixelSize(100, 100), options)
        }
        assertFalse(cache.urls().hasNext())
    }

    @Test
    fun `request on main thread with network cache policy disabled executes correctly`() {
        val fetcher = HttpUriFetcher(callFactory)
        val uri = server.url("/normal.jpg").toString().toUri()

        // Save the image in the disk cache.
        runBlocking {
            val result = fetcher.fetch(pool, uri, PixelSize(100, 100), Options(context))
            (result as SourceResult).source.close()
        }

        assertEquals(uri.toString(), cache.urls().next())

        // Load it from the disk cache on the main thread.
        val result = runBlocking(Dispatchers.Main.immediate) {
            val options = Options(context, networkCachePolicy = CachePolicy.DISABLED)
            fetcher.fetch(pool, uri, PixelSize(100, 100), options)
        }

        assertTrue(result is SourceResult)
        assertEquals(DataSource.DISK, result.dataSource)
    }
}
