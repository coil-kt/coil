package coil3.fetch

import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.network.CacheStrategy
import coil3.request.Options
import coil3.test.RobolectricTest
import coil3.test.context
import coil3.test.runTestAsync
import coil3.toUri
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import okio.blackholeSink
import okio.fakefilesystem.FakeFileSystem
import okio.use

class NetworkFetcherTest : RobolectricTest() {

    private lateinit var fileSystem: FakeFileSystem
    private lateinit var diskCache: DiskCache

    @BeforeTest
    fun before() {
        fileSystem = FakeFileSystem()
        diskCache = DiskCache.Builder()
            .directory(fileSystem.workingDirectory)
            .fileSystem(fileSystem)
            .maxSizeBytes(Long.MAX_VALUE)
            .build()
    }

    @AfterTest
    fun after() {
        diskCache.shutdown()
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun basicNetworkFetch() = runTestAsync {
        val expectedSize = 1_000
        val engine = MockEngine {
            respond(ByteArray(expectedSize))
        }
        val result = newFetcher(engine = engine).fetch()

        assertIs<SourceFetchResult>(result)
        val actualSize = result.source.use { it.source().readAll(blackholeSink()) }
        assertEquals(expectedSize.toLong(), actualSize)
    }

    @Test
    fun mimeTypeIsParsedCorrectlyFromContentType() {
        val fetcher = newFetcher()

        val url1 = "https://example.com/image.jpg"
        val type1 = "image/svg+xml"
        assertEquals(type1, fetcher.getMimeType(url1, type1))

        val url2 = "https://www.example.com/image.svg"
        val type2: String? = null
        assertEquals("image/svg+xml", fetcher.getMimeType(url2, type2))

        val url3 = "https://www.example.com/image"
        val type3 = "image/svg+xml;charset=utf-8"
        assertEquals("image/svg+xml", fetcher.getMimeType(url3, type3))

        val url4 = "https://www.example.com/image.svg"
        val type4 = "text/plain"
        assertEquals("image/svg+xml", fetcher.getMimeType(url4, type4))

        val url5 = "https://www.example.com/image"
        val type5: String? = null
        assertNull(fetcher.getMimeType(url5, type5))
    }

//    @Test
//    fun `request on main thread throws NetworkOnMainThreadException`() = runTest {
//        server.enqueueImage(IMAGE)
//        val url = server.url(IMAGE).toString()
//        val fetcher = newFetcher(url)
//
//        assertFailsWith<NetworkOnMainThreadException> { fetcher.fetch() }
//    }

    @Test
    fun noDiskCache_fetcherReturnsASourceResult() = runTestAsync {
        val expectedSize = 1_000
        val engine = MockEngine {
            respond(ByteArray(expectedSize))
        }
        val result = newFetcher(engine = engine, diskCache = null).fetch()

        assertIs<SourceFetchResult>(result)
        val actualSize = result.source.use { it.source().readAll(blackholeSink()) }
        assertEquals(expectedSize.toLong(), actualSize)
    }

//    @Test
//    fun `request on main thread with network cache policy disabled executes without throwing`() = runTestAsync {
//        val expectedSize = server.enqueueImage(IMAGE)
//        val url = server.url(IMAGE).toString()
//
//        // Write the image in the disk cache.
//        val editor = diskCache.openEditor(url)!!
//        fileSystem.write(editor.data) {
//            writeAll(context.assets.open(IMAGE).source())
//        }
//        editor.commit()
//
//        // Load it from the disk cache on the main thread.
//        val result = newFetcher(
//            url = url,
//            options = Options(context, networkCachePolicy = CachePolicy.DISABLED)
//        ).fetch()
//
//        assertIs<SourceFetchResult>(result)
//        assertNotNull(result.source.fileOrNull())
//        assertEquals(DataSource.DISK, result.dataSource)
//        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
//    }
//
//    @Test
//    fun `no cached file - fetcher returns the file`() = runTestAsync {
//        val expectedSize = server.enqueueImage(IMAGE)
//        val url = server.url(IMAGE).toString()
//        val result = newFetcher(url).fetch()
//
//        assertIs<SourceFetchResult>(result)
//        val source = result.source
//        assertTrue(source is FileImageSource)
//
//        // Ensure we can read the source.
//        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
//
//        // Ensure the result file is present.
//        diskCache.openSnapshot(url)!!.use { snapshot ->
//            assertTrue(snapshot.data in fileSystem.list(diskCache.directory))
//            assertEquals(snapshot.data, source.file)
//        }
//    }
//
//    @Test
//    fun `existing cached file - fetcher returns the file`() = runTestAsync {
//        val url = server.url(IMAGE).toString()
//
//        // Run the fetcher once to create the disk cache file.
//        var expectedSize = server.enqueueImage(IMAGE)
//        var result = newFetcher(url).fetch()
//        assertIs<SourceFetchResult>(result)
//        assertTrue(result.source is FileImageSource)
//        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
//
//        // Run the fetcher a second time.
//        expectedSize = server.enqueueImage(IMAGE)
//        result = newFetcher(url).fetch()
//        assertIs<SourceFetchResult>(result)
//        assertTrue(result.source is FileImageSource)
//        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
//
//        // Ensure the result file is present.
//        val expected = diskCache.openSnapshot(url)?.data
//        assertTrue(expected in fileSystem.list(diskCache.directory))
//        assertEquals(expected, (result.source as FileImageSource).file)
//    }
//
//    @Test
//    fun `cache control - empty metadata is always returned`() = runTestAsync {
//        val url = server.url(IMAGE).toString()
//
//        val editor = diskCache.openEditor(url)!!
//        fileSystem.write(editor.data) {
//            writeAll(context.assets.open(IMAGE).source())
//        }
//        editor.commit()
//
//        val result = newFetcher(url).fetch()
//
//        assertEquals(0, server.requestCount)
//        assertIs<SourceFetchResult>(result)
//        assertEquals(DataSource.DISK, result.dataSource)
//    }
//
//    @Test
//    fun `cache control - no-store is never cached or returned`() = runTestAsync {
//        val url = server.url(IMAGE).toString()
//
//        val headers = Headers.Builder()
//            .set("Cache-Control", "no-store")
//            .build()
//        var expectedSize = server.enqueueImage(IMAGE, headers)
//        var result = newFetcher(url).fetch()
//
//        assertIs<SourceFetchResult>(result)
//        assertEquals(DataSource.NETWORK, result.dataSource)
//        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
//
//        diskCache.openSnapshot(url).use(::assertNull)
//
//        expectedSize = server.enqueueImage(IMAGE, headers)
//        result = newFetcher(url).fetch()
//
//        assertEquals(2, server.requestCount)
//        assertIs<SourceFetchResult>(result)
//        assertEquals(DataSource.NETWORK, result.dataSource)
//        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
//    }
//
//    @Test
//    fun `cache control - respectCacheHeaders=false is always cached and returned`() = runTestAsync {
//        val url = server.url(IMAGE).toString()
//
//        val headers = Headers.Builder()
//            .set("Cache-Control", "no-store")
//            .build()
//        var expectedSize = server.enqueueImage(IMAGE, headers)
//        var result = newFetcher(url, respectCacheHeaders = false).fetch()
//
//        assertIs<SourceFetchResult>(result)
//        assertEquals(DataSource.NETWORK, result.dataSource)
//        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
//
//        diskCache.openSnapshot(url).use(::assertNotNull)
//
//        expectedSize = server.enqueueImage(IMAGE, headers)
//        result = newFetcher(url, respectCacheHeaders = false).fetch()
//
//        assertEquals(1, server.requestCount)
//        assertIs<SourceFetchResult>(result)
//        assertEquals(DataSource.DISK, result.dataSource)
//        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
//    }
//
//    @Test
//    fun `cache control - cached response is verified and returned from the cache`() = runTestAsync {
//        val url = server.url(IMAGE).toString()
//        val etag = UUID.randomUUID().toString()
//        val headers = Headers.Builder()
//            .set("Cache-Control", "no-cache")
//            .set("ETag", etag)
//            .build()
//        val expectedSize = server.enqueueImage(IMAGE, headers)
//        var result = newFetcher(url).fetch()
//
//        assertEquals(1, server.requestCount)
//        assertIs<SourceFetchResult>(result)
//        assertEquals(DataSource.NETWORK, result.dataSource)
//        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
//        diskCache.openSnapshot(url).use(::assertNotNull)
//
//        // Don't set a response body as it should be read from the cache.
//        server.enqueue(MockResponse().setResponseCode(HTTP_NOT_MODIFIED))
//        result = newFetcher(url).fetch()
//
//        assertIs<SourceFetchResult>(result)
//        assertEquals(DataSource.NETWORK, result.dataSource)
//        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
//
//        server.takeRequest() // Discard the first request.
//
//        // Ensure we passed the correct etag.
//        assertEquals(2, server.requestCount)
//        assertEquals(etag, server.takeRequest().headers["If-None-Match"])
//    }
//
//    /** Regression test: https://github.com/coil-kt/coil/issues/1256 */
//    @Test
//    fun `cache control - HTTP_NOT_MODIFIED response combines headers with cached response`() = runTestAsync {
//        val url = server.url(IMAGE).toString()
//        val headers = Headers.Builder()
//            .set("Cache-Control", "no-cache")
//            .set("Cache-Header", "none")
//            .set("ETag", "fake_etag")
//            .build()
//        val expectedSize = server.enqueueImage(IMAGE, headers)
//        var result = newFetcher(url).fetch()
//
//        assertEquals(1, server.requestCount)
//        assertIs<SourceFetchResult>(result)
//        assertEquals(DataSource.NETWORK, result.dataSource)
//        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
//        diskCache.openSnapshot(url).use(::assertNotNull)
//
//        // Don't set a response body as it should be read from the cache.
//        val response = MockResponse()
//            .setResponseCode(HTTP_NOT_MODIFIED)
//            .addHeader("Response-Header", "none")
//        server.enqueue(response)
//        result = newFetcher(url).fetch()
//
//        assertIs<SourceFetchResult>(result)
//        assertEquals(DataSource.NETWORK, result.dataSource)
//        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
//
//        server.takeRequest() // Discard the first request.
//
//        assertEquals(2, server.requestCount)
//        val cacheResponse = diskCache.openSnapshot(url)!!.use { snapshot ->
//            CacheResponse(diskCache.fileSystem.source(snapshot.metadata).buffer())
//        }
//        val expectedHeaders = headers.newBuilder()
//            .addAll(response.headers)
//            // Content-Length is set later by OkHttp.
//            .set("Content-Length", expectedSize.toString())
//            .build()
//        assertEquals(expectedHeaders.toSet(), cacheResponse.responseHeaders.toSet())
//    }
//
//    /** Regression test: https://github.com/coil-kt/coil/issues/1838 */
//    @Test
//    fun `cache control - HTTP_NOT_MODIFIED response combines headers with cached response with non-ASCII cached headers`() = runTestAsync {
//        val url = server.url(IMAGE).toString()
//        val headers = Headers.Builder()
//            .set("Cache-Control", "no-cache")
//            .set("Cache-Header", "none")
//            .set("ETag", "fake_etag")
//            .addUnsafeNonAscii(
//                "Content-Disposition",
//                "inline; filename=\"alimentación.webp\""
//            )
//            .build()
//        val expectedSize = server.enqueueImage(IMAGE, headers)
//        var result = newFetcher(url).fetch()
//
//        assertEquals(1, server.requestCount)
//        assertIs<SourceResult>(result)
//        assertEquals(DataSource.NETWORK, result.dataSource)
//        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
//        diskCache.openSnapshot(url).use(::assertNotNull)
//
//        // Don't set a response body as it should be read from the cache.
//        val response = MockResponse()
//            .setResponseCode(HTTP_NOT_MODIFIED)
//            .addHeader("Response-Header", "none")
//        server.enqueue(response)
//        result = newFetcher(url).fetch()
//
//        assertIs<SourceResult>(result)
//        assertEquals(DataSource.NETWORK, result.dataSource)
//        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
//
//        server.takeRequest() // Discard the first request.
//
//        assertEquals(2, server.requestCount)
//        val cacheResponse = diskCache.openSnapshot(url)!!.use { snapshot ->
//            CacheResponse(diskCache.fileSystem.source(snapshot.metadata).buffer())
//        }
//        val expectedHeaders = headers.newBuilder()
//            .addAll(response.headers)
//            // Content-Length is set later by OkHttp.
//            .set("Content-Length", expectedSize.toString())
//            .build()
//        assertEquals(expectedHeaders.toSet(), cacheResponse.responseHeaders.toSet())
//    }
//
//    @Test
//    fun `cache control - unexpired max-age is returned from cache`() = runTestAsync {
//        val url = server.url(IMAGE).toString()
//
//        val headers = Headers.Builder()
//            .set("Cache-Control", "max-age=60")
//            .build()
//        var expectedSize = server.enqueueImage(IMAGE, headers)
//        var result = newFetcher(url).fetch()
//
//        assertIs<SourceFetchResult>(result)
//        assertEquals(DataSource.NETWORK, result.dataSource)
//        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
//
//        diskCache.openSnapshot(url).use(::assertNotNull)
//
//        expectedSize = server.enqueueImage(IMAGE, headers)
//        result = newFetcher(url).fetch()
//
//        assertEquals(1, server.requestCount)
//        assertIs<SourceFetchResult>(result)
//        assertEquals(DataSource.DISK, result.dataSource)
//        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
//    }
//
//    @Test
//    fun `cache control - expired max-age is not returned from cache`() = runTestAsync {
//        val url = server.url(IMAGE).toString()
//
//        val headers = Headers.Builder()
//            .set("Cache-Control", "max-age=60")
//            .build()
//        var expectedSize = server.enqueueImage(IMAGE, headers)
//        var result = newFetcher(url).fetch()
//
//        assertIs<SourceFetchResult>(result)
//        assertEquals(DataSource.NETWORK, result.dataSource)
//        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
//
//        diskCache.openSnapshot(url).use(::assertNotNull)
//
//        // Increase the current time.
//        clock.currentTimeMillis += 65_000
//
//        expectedSize = server.enqueueImage(IMAGE, headers)
//        result = newFetcher(url).fetch()
//
//        assertEquals(2, server.requestCount)
//        assertIs<SourceFetchResult>(result)
//        assertEquals(DataSource.NETWORK, result.dataSource)
//        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
//    }

    private fun newFetcher(
        url: String = "https://example.com/image.jpg",
        engine: MockEngine = MockEngine { respondOk() },
        cacheStrategy: CacheStrategy = CacheStrategy(),
        options: Options = Options(context),
        diskCache: DiskCache? = this.diskCache,
    ): NetworkFetcher {
        val factory = NetworkFetcher.Factory(
            httpClient = lazyOf(HttpClient(engine)),
            cacheStrategy = lazyOf(cacheStrategy),
        )
        val imageLoader = ImageLoader.Builder(context)
            .diskCache(diskCache)
            .apply { diskCache?.fileSystem?.let(::fileSystem) }
            .build()
        return assertIs(factory.create(url.toUri(), options, imageLoader))
    }
}
