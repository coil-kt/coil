package coil3.network.cachecontrol

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.disk.DiskCache
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.network.CacheResponse
import coil3.network.CacheStrategy
import coil3.network.ConnectivityChecker
import coil3.network.NetworkClient
import coil3.network.NetworkFetcher
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import coil3.request.Options
import coil3.test.utils.context
import coil3.test.utils.runTestAsync
import coil3.toUri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.datetime.Instant
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.blackholeSink
import okio.buffer
import okio.fakefilesystem.FakeFileSystem

class CacheControlCacheStrategyTest {
    private val fileSystem = FakeFileSystem()
    private val diskCache = DiskCache.Builder()
        .fileSystem(fileSystem)
        .directory(fileSystem.workingDirectory)
        .build()
    private val networkClient = FakeNetworkClient()
    private val cacheStrategy = CacheControlCacheStrategy(now = { Instant.DISTANT_PAST })

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
        val response = NetworkResponse(
            headers = headers,
            body = NetworkResponseBody(Buffer().apply { write(FAKE_DATA) }),
        )
        networkClient.enqueue(url, response)
        var result = newFetcher(url).fetch()

        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })

        diskCache.openSnapshot(url).use(::assertNull)

        networkClient.enqueue(url, response)
        result = newFetcher(url).fetch()

        assertEquals(2, networkClient.requests.size)
        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
    }

    @Test
    fun cachedResponseIsVerifiedAndReturnedFromTheCache() = runTestAsync {
        val url = FAKE_URL
        val expectedSize = FAKE_DATA.size.toLong()
        val etag = "fake_etag"
        val headers = NetworkHeaders.Builder()
            .set("Cache-Control", "no-cache")
            .set("ETag", etag)
            .build()
        var response = NetworkResponse(
            headers = headers,
            body = NetworkResponseBody(Buffer().apply { write(FAKE_DATA) }),
        )
        networkClient.enqueue(url, response)
        var result = newFetcher(url).fetch()

        assertEquals(1, networkClient.requests.size)
        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
        diskCache.openSnapshot(url).use(::assertNotNull)

        // Don't set a response body as it should be read from the cache.
        response = NetworkResponse(
            code = 304,
            headers = headers,
            body = NetworkResponseBody(Buffer().apply { write(FAKE_DATA) }),
        )
        networkClient.enqueue(url, response)
        result = newFetcher(url).fetch()

        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })

        // Ensure we passed the correct etag.
        assertEquals(2, networkClient.requests.size)
        assertEquals(etag, networkClient.responses[1].headers["If-None-Match"])
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
        var response = NetworkResponse(
            headers = headers,
            body = NetworkResponseBody(Buffer().apply { write(FAKE_DATA) }),
        )

        networkClient.enqueue(url, response)
        var result = newFetcher(url).fetch()

        assertEquals(1, networkClient.requests.size)
        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
        diskCache.openSnapshot(url).use(::assertNotNull)

        // Don't set a response body as it should be read from the cache.
        response = NetworkResponse(
            code = 304,
            headers = NetworkHeaders.Builder()
                .set("Response-Header", "none")
                .build(),
        )
        networkClient.enqueue(url, response)
        result = newFetcher(url).fetch()

        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })

        assertEquals(2, networkClient.requests.size)
        val cacheResponse = diskCache.openSnapshot(url)!!.use { snapshot ->
            CacheResponse.readFrom(diskCache.fileSystem.source(snapshot.metadata).buffer())
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
    fun HTTP_NOT_MODIFIED_ResponseCombinesHeadersWithCachedResponseWithNonASCII_CachedHeaders() =
        runTestAsync {
            val url = FAKE_URL
            val expectedSize = FAKE_DATA.size.toLong()
            val headers = NetworkHeaders.Builder()
                .set("Cache-Control", "no-cache")
                .set("Cache-Header", "none")
                .set("ETag", "fake_etag")
                .add("Content-Disposition", "inline; filename=\"alimentación.webp\"")
                .build()
            var response = NetworkResponse(
                headers = headers,
                body = NetworkResponseBody(Buffer().apply { write(FAKE_DATA) }),
            )

            networkClient.enqueue(url, response)
            var result = newFetcher(url).fetch()

            assertEquals(1, networkClient.requests.size)
            assertIs<SourceFetchResult>(result)
            assertEquals(DataSource.NETWORK, result.dataSource)
            assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
            diskCache.openSnapshot(url).use(::assertNotNull)

            // Don't set a response body as it should be read from the cache.
            response = NetworkResponse(
                code = 304,
                headers = NetworkHeaders.Builder()
                    .set("Response-Header", "none")
                    .build(),
                body = NetworkResponseBody(Buffer().apply { write(FAKE_DATA) }),
            )
            networkClient.enqueue(url, response)
            result = newFetcher(url).fetch()

            assertIs<SourceFetchResult>(result)
            assertEquals(DataSource.NETWORK, result.dataSource)
            assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })

            assertEquals(2, networkClient.requests.size)
            val cacheResponse = diskCache.openSnapshot(url)!!.use { snapshot ->
                CacheResponse.readFrom(diskCache.fileSystem.source(snapshot.metadata).buffer())
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
        val response = NetworkResponse(
            headers = headers,
            body = NetworkResponseBody(Buffer().apply { write(FAKE_DATA) }),
        )
        networkClient.enqueue(url, response)
        var result = newFetcher(url).fetch()

        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })

        diskCache.openSnapshot(url).use(::assertNotNull)

        networkClient.enqueue(url, response)
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
        var now = 1000L

        val cacheStrategy = CacheControlCacheStrategy(now = { Instant.fromEpochMilliseconds(now) })

        val headers = NetworkHeaders.Builder()
            .set("Cache-Control", "max-age=60")
            .build()
        var response = NetworkResponse(
            headers = headers,
            body = NetworkResponseBody(Buffer().apply { write(FAKE_DATA) }),
        )
        networkClient.enqueue(url, response)
        var result = newFetcher(url, cacheStrategy = cacheStrategy).fetch()

        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })

        diskCache.openSnapshot(url).use(::assertNotNull)

        // Increase the current time.
        now += 65_000

        response = NetworkResponse(
            headers = headers,
            body = NetworkResponseBody(Buffer().apply { write(FAKE_DATA) }),
        )
        networkClient.enqueue(url, response)
        result = newFetcher(url, cacheStrategy = cacheStrategy).fetch()

        assertEquals(2, networkClient.requests.size)
        assertIs<SourceFetchResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(expectedSize, result.source.use { it.source().readAll(blackholeSink()) })
    }

    private fun newFetcher(
        url: String,
        cacheStrategy: CacheStrategy = this.cacheStrategy,
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

    class FakeNetworkClient : NetworkClient {
        private val queue = mutableMapOf<String, ArrayDeque<NetworkResponse>>()

        val requests = mutableListOf<NetworkRequest>()
        val responses = mutableListOf<NetworkResponse>()

        fun enqueue(url: String, response: NetworkResponse) {
            queue.getOrPut(url, ::ArrayDeque) += response
        }

        override suspend fun <T> executeRequest(
            request: NetworkRequest,
            block: suspend (response: NetworkResponse) -> T,
        ): T {
            requests += request
            val response = queue.getValue(request.url).removeFirst()
            responses += response
            return block(response)
        }
    }

    companion object {
        private const val FAKE_URL = "https://www.example.com/image.jpg"
        private val FAKE_DATA = "ONE-TWO-THREE".encodeUtf8()
    }
}
