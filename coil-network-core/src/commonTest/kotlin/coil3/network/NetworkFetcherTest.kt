package coil3.network

import coil3.Extras
import coil3.disk.DiskCache
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import coil3.test.utils.runTestAsync
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import okio.Buffer
import okio.ByteString.Companion.toByteString
import okio.fakefilesystem.FakeFileSystem

class NetworkFetcherTest : RobolectricTest() {

    @Test
    fun networkRequestParamsArePassedThrough() = runTestAsync {
        val expectedSize = 1_000
        val url = "https://example.com/image.jpg"
        val method = "POST"
        val headers = NetworkHeaders.Builder()
            .set("key", "value")
            .build()
        val body = NetworkRequestBody(ByteArray(500).toByteString())
        val options = Options(
            context = context,
            extras = Extras.Builder()
                .set(Extras.Key.httpMethod, method)
                .set(Extras.Key.httpHeaders, headers)
                .set(Extras.Key.httpBody, body)
                .build(),
        )
        val networkClient = FakeNetworkClient(
            respond = {
                NetworkResponse(
                    body = NetworkResponseBody(
                        source = Buffer().apply { write(ByteArray(expectedSize)) },
                    ),
                )
            },
        )
        val result = NetworkFetcher(
            url = url,
            options = options,
            networkClient = lazyOf(networkClient),
            diskCache = lazyOf(null),
            cacheStrategy = lazyOf(CacheStrategy.DEFAULT),
            connectivityChecker = ConnectivityChecker(context),
            inFlightRequestStrategy = lazyOf(InFlightRequestStrategy.DEFAULT)
        ).fetch()

        assertIs<SourceFetchResult>(result)

        val expected = NetworkRequest(url, method, headers, body, options.extras)

        assertEquals(expected, networkClient.requests.single())
    }

    @Test
    fun error404ResponseIsCachedToDisk() = runTestAsync {
        val expectedSize = 1_000
        val url = "https://example.com/error.jpg"

        val fileSystem = FakeFileSystem()
        val diskCache = DiskCache.Builder()
            .directory(fileSystem.workingDirectory)
            .fileSystem(fileSystem)
            .maxSizeBytes(Long.MAX_VALUE)
            .build()

        val networkClient = FakeNetworkClient(
            respond = {
                NetworkResponse(
                    code = 404,
                    body = NetworkResponseBody(
                        source = Buffer().apply { write(ByteArray(expectedSize)) },
                    ),
                )
            },
        )

        val fetcher = NetworkFetcher(
            url = url,
            options = Options(context),
            networkClient = lazyOf(networkClient),
            diskCache = lazyOf(diskCache),
            cacheStrategy = lazyOf(CacheStrategy.DEFAULT),
            connectivityChecker = ConnectivityChecker.ONLINE,
        )

        // A 400 response throws, but should still be cached.
        assertFailsWith<HttpException> { fetcher.fetch() }

        diskCache.openSnapshot(url)!!.use { snapshot ->
            // Verify the cached metadata records the 400 status code.
            val cachedResponse = diskCache.fileSystem.read(snapshot.metadata) {
                CacheNetworkResponse.readFrom(this)
            }
            assertEquals(404, cachedResponse.code)

            // Verify the cached data exists and has the expected size.
            val actualSize = fileSystem.read(snapshot.data) { readByteString().size }
            assertEquals(expectedSize, actualSize)
        }

        diskCache.shutdown()
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun error500ResponseIsNotCachedToDisk() = runTestAsync {
        val expectedSize = 1_000
        val url = "https://example.com/error-500.jpg"

        val fileSystem = FakeFileSystem()
        val diskCache = DiskCache.Builder()
            .directory(fileSystem.workingDirectory)
            .fileSystem(fileSystem)
            .maxSizeBytes(Long.MAX_VALUE)
            .build()

        val networkClient = FakeNetworkClient(
            respond = {
                NetworkResponse(
                    code = 500,
                    body = NetworkResponseBody(
                        source = Buffer().apply { write(ByteArray(expectedSize)) },
                    ),
                )
            },
        )

        val fetcher = NetworkFetcher(
            url = url,
            options = Options(context),
            networkClient = lazyOf(networkClient),
            diskCache = lazyOf(diskCache),
            cacheStrategy = lazyOf(CacheStrategy.DEFAULT),
            connectivityChecker = ConnectivityChecker.ONLINE,
        )

        // A 500 response throws and should not be cached.
        assertFailsWith<HttpException> { fetcher.fetch() }

        // Verify no snapshot was written for the 500 response.
        assertEquals(null, diskCache.openSnapshot(url))

        diskCache.shutdown()
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun cached404ResponseThrows() = runTestAsync {
        val url = "https://example.com/cached-404.jpg"

        val fileSystem = FakeFileSystem()
        val diskCache = DiskCache.Builder()
            .directory(fileSystem.workingDirectory)
            .fileSystem(fileSystem)
            .maxSizeBytes(Long.MAX_VALUE)
            .build()

        // Pre-populate the disk cache with a cached 404 response.
        val editor = diskCache.openEditor(url)!!
        fileSystem.write(editor.metadata) {
            CacheNetworkResponse.writeTo(NetworkResponse(code = 404), this)
        }

        // Write some data as well to ensure it's a complete entry.
        fileSystem.write(editor.data) {
            write(ByteArray(32))
        }
        editor.commit()

        val networkClient = FakeNetworkClient(
            respond = {
                // Should not be invoked since we throw on cached 404 before network.
                NetworkResponse(
                    body = NetworkResponseBody(Buffer().apply { write(ByteArray(1)) }),
                )
            },
        )

        val fetcher = NetworkFetcher(
            url = url,
            options = Options(context),
            networkClient = lazyOf(networkClient),
            diskCache = lazyOf(diskCache),
            cacheStrategy = lazyOf(CacheStrategy.DEFAULT),
            connectivityChecker = ConnectivityChecker.ONLINE,
        )

        assertFailsWith<HttpException> { fetcher.fetch() }
        assertEquals(0, networkClient.requests.size)

        diskCache.shutdown()
        fileSystem.checkNoOpenFiles()
    }

    class FakeNetworkClient(
        private val respond: suspend (NetworkRequest) -> NetworkResponse,
    ) : NetworkClient {
        val requests = mutableListOf<NetworkRequest>()
        val responses = mutableListOf<NetworkResponse>()

        override suspend fun <T> executeRequest(
            request: NetworkRequest,
            block: suspend (response: NetworkResponse) -> T,
        ): T {
            requests += request
            val response = respond(request)
            responses += response
            return block(response)
        }
    }
}
