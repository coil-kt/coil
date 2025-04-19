package coil3.network.cachecontrol

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.disk.DiskCache
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.network.CacheNetworkResponse
import coil3.network.CacheStrategy
import coil3.network.ConnectivityChecker
import coil3.network.NetworkClient
import coil3.network.NetworkFetcher
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import coil3.request.Options
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import coil3.test.utils.runTestAsync
import coil3.toUri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Instant
import okio.Buffer
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8
import okio.blackholeSink
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import okio.use

class CacheControlCacheStrategyTest : RobolectricTest() {
    // Friday, November 8, 2024 8:00:00 AM UTC
    private var now = Instant.fromEpochSeconds(1731052800)

    private val fileSystem = FakeFileSystem()
    private val diskCache = DiskCache.Builder()
        .fileSystem(fileSystem)
        .directory(fileSystem.workingDirectory)
        .build()
    private val networkClient = FakeNetworkClient()
    private val cacheStrategy = CacheControlCacheStrategy(now = { now })

    @Test
    fun emptyMetadataIsAlwaysReturned() = runTestAsync {
        val url = FAKE_URL

        val editor = diskCache.openEditor(url)!!
        fileSystem.write(editor.data) {
            write(FAKE_DATA)
        }
        editor.commit()

        val result = newFetcher(url).fetch()

        assertEquals(0, networkClient.requests.size)
        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.DISK, result.dataSource)
    }

    @Test
    fun noStoreIsNeverCachedOrReturned() = runTestAsync {
        val url = FAKE_URL
        val expectedSize = FAKE_DATA.size.toLong()
        val headers = NetworkHeaders.Builder()
            .set("Cache-Control", "no-store")
            .build()
        var response = FakeNetworkResponse(headers = headers)
        networkClient.enqueue(url, response)
        var result = newFetcher(url).fetch()

        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })

        response = FakeNetworkResponse(headers = headers)
        networkClient.enqueue(url, response)
        result = newFetcher(url).fetch()

        assertEquals(2, networkClient.requests.size)
        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
    }

    @Test
    fun etagIsVerified_304() = runTestAsync {
        val url = FAKE_URL
        val expectedSize = FAKE_DATA.size.toLong()
        val etag = "fake_etag"
        val headers = NetworkHeaders.Builder()
            .set("Cache-Control", "no-cache")
            .set("ETag", etag)
            .build()
        var response = FakeNetworkResponse(headers = headers)
        networkClient.enqueue(url, response)
        var result = newFetcher(url).fetch()

        assertEquals(1, networkClient.requests.size)
        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(FAKE_DATA, result.source.use { it.source().readByteString() })

        // Assert is was persisted to the disk cache with the correct etag and data.
        diskCache.openSnapshot(url)!!.use {
            val cachedResponse = fileSystem.source(it.metadata).buffer().use(CacheNetworkResponse::readFrom)
            assertEquals(etag, cachedResponse.headers["ETag"])
            assertEquals(FAKE_DATA, fileSystem.source(it.data).buffer().use(BufferedSource::readByteString))
        }

        // Don't set a response body as it should be read from the cache.
        response = FakeNetworkResponse(
            code = 304,
            headers = headers,
            body = null,
        )
        networkClient.enqueue(url, response)
        result = newFetcher(url).fetch()

        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })

        // Ensure we passed the correct etag.
        assertEquals(2, networkClient.requests.size)
        assertEquals(etag, networkClient.requests[1].headers["If-None-Match"])
    }

    @Test
    fun etagIsVerified_different() = runTestAsync {
        val url = FAKE_URL
        val newHeaders = { etag: String ->
            NetworkHeaders.Builder()
                .set("Cache-Control", "no-cache")
                .set("ETag", etag)
                .build()
        }
        var data = "abcdefg".encodeUtf8()
        var response = FakeNetworkResponse(
            headers = newHeaders("first_etag"),
            body = NetworkResponseBody(Buffer().apply { write(data) }),
        )
        networkClient.enqueue(url, response)
        var result = newFetcher(url).fetch()

        assertEquals(1, networkClient.requests.size)
        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(data, result.source.use { it.source().readByteString() })

        // Assert is was persisted to the disk cache with the correct etag and data.
        diskCache.openSnapshot(url)!!.use {
            val cachedResponse = fileSystem.source(it.metadata).buffer().use(CacheNetworkResponse::readFrom)
            assertEquals("first_etag", cachedResponse.headers["ETag"])
            assertEquals(data, fileSystem.source(it.data).buffer().use(BufferedSource::readByteString))
        }

        // Don't set a response body as it should be read from the cache.
        data = "1234567".encodeUtf8()
        response = FakeNetworkResponse(
            code = 200,
            headers = newHeaders("second_etag"),
            body = NetworkResponseBody(Buffer().apply { write(data) }),
        )
        networkClient.enqueue(url, response)
        result = newFetcher(url).fetch()

        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(data, result.source.use { it.source().readByteString() })

        // Ensure we passed the correct etag.
        assertEquals(2, networkClient.requests.size)
        assertEquals("first_etag", networkClient.requests[1].headers["If-None-Match"])

        // Assert is was persisted to the disk cache with the correct etag and data.
        diskCache.openSnapshot(url)!!.use {
            val cachedResponse = fileSystem.source(it.metadata).buffer().use(CacheNetworkResponse::readFrom)
            assertEquals("second_etag", cachedResponse.headers["ETag"])
            assertEquals(data, fileSystem.source(it.data).buffer().use(BufferedSource::readByteString))
        }
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1256 */
    @Test
    fun HTTP_NOT_MODIFIED_ResponseCombinesHeadersWithCachedResponse() = runTestAsync {
        val url = FAKE_URL
        val expectedSize = FAKE_DATA.size.toLong()
        val headers = NetworkHeaders.Builder()
            .set("Cache-Control", "no-cache")
            .set("Cache-Header", "none")
            .set("ETag", "fake_etag")
            .build()
        var response = FakeNetworkResponse(headers = headers)

        networkClient.enqueue(url, response)
        var result = newFetcher(url).fetch()

        assertEquals(1, networkClient.requests.size)
        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
        diskCache.openSnapshot(url).use(::assertNotNull)

        // Don't set a response body as it should be read from the cache.
        response = FakeNetworkResponse(
            code = 304,
            headers = NetworkHeaders.Builder()
                .set("Response-Header", "none")
                .build(),
            body = null,
        )
        networkClient.enqueue(url, response)
        result = newFetcher(url).fetch()

        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })

        assertEquals(2, networkClient.requests.size)
        val cacheResponse = diskCache.openSnapshot(url)!!.use { snapshot ->
            CacheNetworkResponse.readFrom(diskCache.fileSystem.source(snapshot.metadata).buffer())
        }
        val expectedNetworkHeaders = headers.newBuilder()
            .apply {
                for ((name, values) in response.headers.asMap()) {
                    for (value in values) add(name, value)
                }
            }
            .build()
        assertEquals(expectedNetworkHeaders.asMap(), cacheResponse.headers.asMap())
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1838 */
    @Test
    fun HTTP_NOT_MODIFIED_ResponseCombinesHeadersWithCachedResponseWithNonASCII_CachedHeaders() = runTestAsync {
        val url = FAKE_URL
        val expectedSize = FAKE_DATA.size.toLong()
        val headers = NetworkHeaders.Builder()
            .set("Cache-Control", "no-cache")
            .set("Cache-Header", "none")
            .set("ETag", "fake_etag")
            .add("Content-Disposition", "inline; filename=\"alimentación.webp\"")
            .build()
        var response = FakeNetworkResponse(headers = headers)

        networkClient.enqueue(url, response)
        var result = newFetcher(url).fetch()

        assertEquals(1, networkClient.requests.size)
        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
        diskCache.openSnapshot(url).use(::assertNotNull)

        // Don't set a response body as it should be read from the cache.
        response = FakeNetworkResponse(
            code = 304,
            headers = NetworkHeaders.Builder()
                .set("Response-Header", "none")
                .build(),
            body = null,
        )
        networkClient.enqueue(url, response)
        result = newFetcher(url).fetch()

        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })

        assertEquals(2, networkClient.requests.size)
        val cacheResponse = diskCache.openSnapshot(url)!!.use { snapshot ->
            CacheNetworkResponse.readFrom(diskCache.fileSystem.source(snapshot.metadata).buffer())
        }
        val expectedNetworkHeaders = headers.newBuilder()
            .apply {
                for ((name, values) in response.headers.asMap()) {
                    for (value in values) add(name, value)
                }
            }
            .build()
        assertEquals(expectedNetworkHeaders.asMap(), cacheResponse.headers.asMap())
    }

    @Test
    fun unexpiredMaxAgeIsReturnedFromCache() = runTestAsync {
        val url = FAKE_URL
        val expectedSize = FAKE_DATA.size.toLong()

        val headers = NetworkHeaders.Builder()
            .set("Cache-Control", "max-age=60")
            .build()
        val response = FakeNetworkResponse(headers = headers)
        networkClient.enqueue(url, response)
        var result = newFetcher(url).fetch()

        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })

        diskCache.openSnapshot(url).use(::assertNotNull)

        result = newFetcher(url).fetch()

        assertEquals(1, networkClient.requests.size)
        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.DISK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
    }

    @Test
    fun expiredMaxAgeIsNotReturnedFromCache() = runTestAsync {
        val url = FAKE_URL
        val expectedSize = FAKE_DATA.size.toLong()

        val headers = NetworkHeaders.Builder()
            .set("Cache-Control", "max-age=60")
            .build()
        var response = FakeNetworkResponse(headers = headers)
        networkClient.enqueue(url, response)
        var result = newFetcher(url).fetch()

        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })

        diskCache.openSnapshot(url).use(::assertNotNull)

        // Increase the current time.
        now += 65.seconds

        response = FakeNetworkResponse(headers = headers)
        networkClient.enqueue(url, response)
        result = newFetcher(url).fetch()

        assertEquals(2, networkClient.requests.size)
        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
    }

    @Test
    fun expiredHeaderIsNotReturnedFromCache() = runTestAsync {
        now = Instant.fromEpochMilliseconds(1723436400000L)

        val url = FAKE_URL
        val expectedSize = FAKE_DATA.size.toLong()

        val headers = NetworkHeaders.Builder()
            .set("Cache-Control", "public")
            .set("Expires", "Fri, 9 Aug 2024 12:00:00 GMT")
            .build()
        var response = FakeNetworkResponse(headers = headers)
        networkClient.enqueue(url, response)
        var result = newFetcher(url).fetch()

        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })

        diskCache.openSnapshot(url).use(::assertNotNull)

        response = FakeNetworkResponse(headers = headers)
        networkClient.enqueue(url, response)
        result = newFetcher(url).fetch()

        assertEquals(2, networkClient.requests.size)
        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
    }

    @Test
    fun expired0IsNotReturnedFromCache() = runTestAsync {
        now = Instant.fromEpochMilliseconds(1723436400000L)

        val url = FAKE_URL
        val expectedSize = FAKE_DATA.size.toLong()

        val headers = NetworkHeaders.Builder()
            .set("Cache-Control", "public")
            .set("Expires", "0") // otherwise identical to the test with the date format
            .build()
        var response = FakeNetworkResponse(headers = headers)
        networkClient.enqueue(url, response)
        var result = newFetcher(url).fetch()

        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })

        diskCache.openSnapshot(url).use(::assertNotNull)

        response = FakeNetworkResponse(headers = headers)
        networkClient.enqueue(url, response)
        result = newFetcher(url).fetch()

        assertEquals(2, networkClient.requests.size)
        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
    }

    @Test
    fun unexpiredHeaderIsReturnedFromCache() = runTestAsync {
        now = Instant.fromEpochMilliseconds(1623436400000L)

        val url = FAKE_URL
        val expectedSize = FAKE_DATA.size.toLong()

        val headers = NetworkHeaders.Builder()
            .set("Cache-Control", "public")
            .set("Expires", "Fri, 9 Aug 2024 12:00:00 GMT")
            .build()
        var response = FakeNetworkResponse(headers = headers)
        networkClient.enqueue(url, response)
        var result = newFetcher(url).fetch()

        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })

        diskCache.openSnapshot(url).use(::assertNotNull)

        // Don't set a response body as it should be read from the cache.
        response = FakeNetworkResponse(
            headers = headers,
            body = null,
        )
        networkClient.enqueue(url, response)
        result = newFetcher(url).fetch()

        assertEquals(1, networkClient.requests.size)
        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.DISK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/2935 */
    @Test
    fun headersAreParsedLazily() = runTestAsync {
        val url = FAKE_URL
        val headers = NetworkHeaders.Builder()
            .set("Cache-Control", "max-age=2592000, s-maxage=2592000, public")
            .set("Date", "Thu, 10 Apr 2025 13:15:20 GMT")
            .set("Last-Modified", "Thu, 1 Jan 2000 00:00:00 GMT")
            .build()
        val response = FakeNetworkResponse(headers = headers)

        val result = cacheStrategy.read(
            cacheResponse = response,
            networkRequest = NetworkRequest(url),
            options = Options(context),
        )

        assertEquals(CacheStrategy.ReadResult(response), result)
    }

    private fun newFetcher(
        url: String,
        networkClient: NetworkClient = this.networkClient,
        diskCache: DiskCache? = this.diskCache,
        options: Options = Options(context),
    ): Fetcher {
        val factory = NetworkFetcher.Factory(
            networkClient = { networkClient },
            cacheStrategy = { cacheStrategy },
            connectivityChecker = { ConnectivityChecker.ONLINE },
        )
        val imageLoader = ImageLoader.Builder(context)
            .diskCache(diskCache)
            .build()
        return checkNotNull(factory.create(url.toUri(), options, imageLoader)) { "fetcher == null" }
    }

    private fun FakeNetworkResponse(
        code: Int = 200,
        requestMillis: Long = now.toEpochMilliseconds(),
        responseMillis: Long = now.toEpochMilliseconds(),
        headers: NetworkHeaders = NetworkHeaders.EMPTY,
        body: NetworkResponseBody? = NetworkResponseBody(Buffer().apply { write(FAKE_DATA) }),
        delegate: Any? = null,
    ) = NetworkResponse(
        code = code,
        requestMillis = requestMillis,
        responseMillis = responseMillis,
        headers = headers,
        body = body,
        delegate = delegate,
    )

    class FakeNetworkClient : NetworkClient {
        private val queues = mutableMapOf<String, ArrayDeque<NetworkResponse>>()

        val requests = mutableListOf<NetworkRequest>()
        val responses = mutableListOf<NetworkResponse>()

        fun enqueue(url: String, response: NetworkResponse) {
            queues.getOrPut(url, ::ArrayDeque) += response
        }

        override suspend fun <T> executeRequest(
            request: NetworkRequest,
            block: suspend (response: NetworkResponse) -> T,
        ): T {
            requests += request
            val response = queues.getValue(request.url).removeFirst()
            responses += response
            return block(response)
        }
    }

    companion object {
        private const val FAKE_URL = "https://www.example.com/image.jpg"
        private val FAKE_DATA = "ONE-TWO-THREE".encodeUtf8()
    }
}
