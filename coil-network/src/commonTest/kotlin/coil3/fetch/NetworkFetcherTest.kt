package coil3.fetch

class NetworkFetcherTest {
//
//    private lateinit var server: MockWebServer
//    private lateinit var fileSystem: FileSystem
//    private lateinit var diskCache: DiskCache
//    private lateinit var clock: FakeClock
//    private lateinit var callFactory: Call.Factory
//    private lateinit var imageLoader: ImageLoader
//
//    @BeforeTest
//    fun before() {
//        server = createMockWebServer()
//        fileSystem = FileSystem.SYSTEM
//        diskCache = DiskCache.Builder()
//            .directory(File("build/cache"))
//            .maxSizeBytes(10L * 1024 * 1024) // 10MB
//            .build()
//        clock = FakeClock()
//        callFactory = OkHttpClient()
//        imageLoader = ImageLoader.Builder(context)
//            .callFactory(callFactory)
//            .diskCache(diskCache)
//            .build()
//    }
//
//    @AfterTest
//    fun after() {
//        server.shutdown()
//        imageLoader.shutdown()
//        diskCache.clear()
//        fileSystem.deleteRecursively(diskCache.directory) // Ensure we start fresh.
//    }
//
//    @Test
//    fun `basic network fetch`() = runTestAsync {
//        val expectedSize = server.enqueueImage(IMAGE)
//        val url = server.url(IMAGE).toString()
//        val result = newFetcher(url).fetch()
//
//        assertIs<SourceFetchResult>(result)
//        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
//    }
//
//    @Test
//    fun `mime type is parsed correctly from content type`() {
//        val fetcher = NetworkFetcher(
//            url = "error",
//            options = Options(context),
//            callFactory = lazyOf(callFactory),
//            diskCache = lazyOf(diskCache),
//            respectCacheHeaders = true
//        )
//
//        // https://android.googlesource.com/platform/frameworks/base/+/61ae88e/core/java/android/webkit/MimeTypeMap.java#407
//        Shadows.shadowOf(MimeTypeMap.getSingleton())
//            .addExtensionMimeTypeMapping("svg", "image/svg+xml")
//
//        val url1 = "https://example.com/image.jpg"
//        val type1 = "image/svg+xml".toMediaType()
//        assertEquals("image/svg+xml", fetcher.getMimeType(url1, type1))
//
//        val url2 = "https://www.example.com/image.svg"
//        val type2: MediaType? = null
//        assertEquals("image/svg+xml", fetcher.getMimeType(url2, type2))
//
//        val url3 = "https://www.example.com/image"
//        val type3 = "image/svg+xml;charset=utf-8".toMediaType()
//        assertEquals("image/svg+xml", fetcher.getMimeType(url3, type3))
//
//        val url4 = "https://www.example.com/image.svg"
//        val type4 = "text/plain".toMediaType()
//        assertEquals("image/svg+xml", fetcher.getMimeType(url4, type4))
//
//        val url5 = "https://www.example.com/image"
//        val type5: MediaType? = null
//        assertNull(fetcher.getMimeType(url5, type5))
//    }
//
//    @Test
//    fun `request on main thread throws NetworkOnMainThreadException`() = runTest {
//        server.enqueueImage(IMAGE)
//        val url = server.url(IMAGE).toString()
//        val fetcher = newFetcher(url)
//
//        assertFailsWith<NetworkOnMainThreadException> { fetcher.fetch() }
//    }
//
//    @Test
//    fun `no disk cache - fetcher returns a source result`() = runTestAsync {
//        val expectedSize = server.enqueueImage(IMAGE)
//        val url = server.url(IMAGE).toString()
//        val result = newFetcher(url, diskCache = null).fetch()
//
//        assertIs<SourceFetchResult>(result)
//        assertIs<SourceImageSource>(result.source)
//        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
//    }
//
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
//
//    private fun newFetcher(
//        url: String,
//        options: Options = Options(context),
//        callFactory: Call.Factory = this.callFactory,
//        diskCache: DiskCache? = this.diskCache,
//        clock: Clock = this.clock,
//        respectCacheHeaders: Boolean = true,
//    ): Fetcher {
//        val factory = NetworkFetcher.Factory(lazyOf(callFactory), lazyOf(diskCache), clock, respectCacheHeaders)
//        return checkNotNull(factory.create(url.toUri(), options, imageLoader)) { "fetcher == null" }
//    }
//
//    companion object {
//        private const val IMAGE = "normal.jpg"
//    }
}
