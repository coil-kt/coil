package coil3.network

import coil3.Extras
import coil3.fetch.SourceFetchResult
import coil3.disk.DiskCache
import coil3.request.Options
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import coil3.test.utils.runTestAsync
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFailsWith
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
        ).fetch()

        assertIs<SourceFetchResult>(result)

        val expected = NetworkRequest(url, method, headers, body, options.extras)

        assertEquals(expected, networkClient.requests.single())
    }

    @Test
    fun errorResponseIsCachedToDisk() = runTestAsync {
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
                    code = 400,
                    body = NetworkResponseBody(
                        source = Buffer().apply { write(ByteArray(expectedSize)) },
                    ),
                )
            },
        )

        val fetcher = NetworkFetcher(
            url = url,
            options = Options(context = context),
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
            assertEquals(400, cachedResponse.code)

            // Verify the cached data exists and has the expected size.
            val actualSize = fileSystem.read(snapshot.data) { readByteString().size }
            assertEquals(expectedSize, actualSize)
        }

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
