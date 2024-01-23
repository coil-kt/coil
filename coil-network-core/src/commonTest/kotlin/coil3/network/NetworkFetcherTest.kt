package coil3.network

import coil3.Extras
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import coil3.test.utils.runTestAsync
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import okio.Buffer
import okio.ByteString.Companion.toByteString

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
            respond = { request ->
                NetworkResponse(
                    request = request,
                    body = NetworkResponseBody(Buffer().apply { write(ByteArray(expectedSize)) }),
                )
            },
        )
        val result = NetworkFetcher(
            url = url,
            options = options,
            networkClient = lazyOf(networkClient),
            diskCache = lazyOf(null),
            cacheStrategy = lazyOf(CacheStrategy()),
        ).fetch()

        assertIs<SourceFetchResult>(result)

        val expected = NetworkRequest(url, method, headers, body)

        assertEquals(expected, networkClient.requests.single())
        assertEquals(expected, networkClient.responses.single().request)
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
