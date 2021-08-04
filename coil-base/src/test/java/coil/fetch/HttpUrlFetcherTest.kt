package coil.fetch

import android.content.Context
import android.os.NetworkOnMainThreadException
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.FileImageSource
import coil.decode.SourceImageSource
import coil.network.HttpException
import coil.network.imageLoaderDiskCache
import coil.request.CachePolicy
import coil.request.Options
import coil.size.PixelSize
import coil.util.CoilUtils
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class HttpUrlFetcherTest {

    private lateinit var diskCache: Cache
    private lateinit var context: Context
    private lateinit var mainDispatcher: TestCoroutineDispatcher
    private lateinit var server: MockWebServer
    private lateinit var callFactory: Call.Factory

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        mainDispatcher = createTestMainDispatcher()
        server = createMockWebServer(context, "normal.jpg", "normal.jpg")
        diskCache = Cache(File("build/cache"), Long.MAX_VALUE).apply { evictAll() }
        assertTrue(diskCache.directory.list().contentEquals(arrayOf("journal")))
        callFactory = OkHttpClient.Builder().imageLoaderDiskCache(diskCache).build()
    }

    @After
    fun after() {
        Dispatchers.resetMain()
        server.shutdown()
        diskCache.evictAll()
    }

    @Test
    fun `basic network URL fetch`() {
        val url = server.url("/normal.jpg")
        val fetcherFactory = HttpUrlFetcher.Factory(callFactory)
        val options = Options(context, size = PixelSize(100, 100))
        val fetcher = assertNotNull(fetcherFactory.create(url, options, ImageLoader(context)))
        val result = runBlocking { fetcher.fetch() }

        assertTrue(result is SourceResult)
    }

    @Test
    fun `basic network URI fetch`() {
        val uri = server.url("/normal.jpg").toString().toUri()
        val fetcherFactory = HttpUrlFetcher.Factory(callFactory)
        val options = Options(context, size = PixelSize(100, 100))
        val fetcher = assertNotNull(fetcherFactory.create(uri, options, ImageLoader(context)))
        val result = runBlocking { fetcher.fetch() }

        assertTrue(result is SourceResult)
    }

    @Test
    fun `mime type is parsed correctly from content type`() {
        val fetcher = HttpUrlFetcher(Unit, Options(context), callFactory)

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
        val options = Options(context, size = PixelSize(100, 100))
        val fetcher = assertNotNull(HttpUrlFetcher.Factory(callFactory).create(uri, options, ImageLoader(context)))

        runBlocking {
            assertFailsWith<HttpException> { fetcher.fetch() }
        }
        assertEquals(uri.toString(), diskCache.urls().next())
    }

    @Test
    fun `request on main thread throws NetworkOnMainThreadException`() = runBlockingTest {
        val uri = server.url("/normal.jpg").toString().toUri()
        val options = Options(context, size = PixelSize(100, 100))
        val fetcher = assertNotNull(HttpUrlFetcher.Factory(callFactory).create(uri, options, ImageLoader(context)))

        assertFailsWith<NetworkOnMainThreadException> { fetcher.fetch() }
        assertFalse(diskCache.urls().hasNext())
    }

    @Test
    fun `no disk cache - fetcher returns a source result`() {
        val url = server.url("/normal.jpg")
        val uri = url.toString().toUri()
        val options = Options(context, size = PixelSize(100, 100))
        val fetcherFactory = HttpUrlFetcher.Factory(OkHttpClient())
        val result = runBlocking {
            assertNotNull(fetcherFactory.create(uri, options, ImageLoader(context))).fetch()
        }

        assertTrue(result is SourceResult)
        assertTrue(result.source is SourceImageSource)
    }

    @Test
    fun `request on main thread with network cache policy disabled executes correctly`() {
        val uri = server.url("/normal.jpg").toString().toUri()
        val options = Options(context, size = PixelSize(100, 100))
        val fetcherFactory = HttpUrlFetcher.Factory(callFactory)

        // Save the image in the disk cache.
        var result = runBlocking {
            assertNotNull(fetcherFactory.create(uri, options, ImageLoader(context))).fetch()
        }
        (result as SourceResult).source.close()

        assertEquals(uri.toString(), diskCache.urls().next())

        // Load it from the disk cache on the main thread.
        result = runBlocking(Dispatchers.Main.immediate) {
            val newOptions = options.copy(networkCachePolicy = CachePolicy.DISABLED)
            assertNotNull(fetcherFactory.create(uri, newOptions, ImageLoader(context))).fetch()
        }

        assertTrue(result is SourceResult)
        assertNotNull(result.source.fileOrNull())
        assertEquals(DataSource.DISK, result.dataSource)
    }

    @Test
    fun `no cached file - fetcher returns the file`() {
        val url = server.url("/normal.jpg")
        val uri = url.toString().toUri()
        val options = Options(context, size = PixelSize(100, 100))
        val fetcherFactory = HttpUrlFetcher.Factory(callFactory)
        val result = runBlocking {
            assertNotNull(fetcherFactory.create(uri, options, ImageLoader(context))).fetch()
        }

        assertTrue(result is SourceResult)
        val source = result.source
        assertTrue(source is FileImageSource)
        val expected = CoilUtils.getDiskCacheFile(diskCache, url)
        assertTrue(expected in diskCache.directory.listFiles().orEmpty())
        assertEquals(expected, source.resultFile)
    }

    @Test
    fun `existing cached file - fetcher returns the file`() {
        val url = server.url("/normal.jpg")
        val uri = url.toString().toUri()
        val options = Options(context, size = PixelSize(100, 100))
        val fetcherFactory = HttpUrlFetcher.Factory(callFactory)

        // Run the fetcher once to create the disk cache file.
        var result = runBlocking {
            assertNotNull(fetcherFactory.create(uri, options, ImageLoader(context))).fetch()
        }
        (result as SourceResult).source.close()

        // Run the fetcher a second time.
        result = runBlocking {
            assertNotNull(fetcherFactory.create(uri, options, ImageLoader(context))).fetch()
        }

        assertTrue(result is SourceResult)
        val source = result.source
        assertTrue(source is FileImageSource)
        val expected = CoilUtils.getDiskCacheFile(diskCache, url)
        assertTrue(expected in diskCache.directory.listFiles().orEmpty())
        assertEquals(expected, source.resultFile)
    }
}
