package coil3.network

import coil3.test.utils.runTestAsync
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async

class DeferredNetworkClientTest {

    @Test
    fun doesNotResolveUntilFirstRequest() = runTestAsync {
        var resolveCount = 0
        val delegate = RecordingNetworkClient()
        val client = deferredNetworkClient {
            resolveCount++
            delegate
        }

        assertEquals(0, resolveCount)

        client.executeRequest(NetworkRequest("https://example.com/image.jpg")) {}

        assertEquals(1, resolveCount)
        assertEquals(1, delegate.requests.size)
    }

    @Test
    fun delegatesRequestAndReturnsBlockResult() = runTestAsync {
        val delegate = RecordingNetworkClient(responseCode = 201)
        val client = deferredNetworkClient { delegate }

        val request = NetworkRequest("https://example.com/image.jpg")
        val code = client.executeRequest(request) { response -> response.code }

        assertEquals(request, delegate.requests.single())
        assertEquals(201, code)
    }

    @Test
    fun suspendsUntilUnderlyingClientIsAvailable() = runTestAsync {
        val deferred = CompletableDeferred<NetworkClient>()
        val client = deferredNetworkClient(deferred::await)

        val request = NetworkRequest("https://example.com/image.jpg")
        // The request can only complete once the underlying client is resolved; awaiting the
        // result below would hang forever if `deferredNetworkClient` didn't suspend on `await`.
        val result = async {
            client.executeRequest(request) { response -> response.code }
        }

        val delegate = RecordingNetworkClient(responseCode = 200)
        deferred.complete(delegate)

        assertEquals(200, result.await())
        assertEquals(request, delegate.requests.single())
    }

    private class RecordingNetworkClient(
        private val responseCode: Int = 200,
    ) : NetworkClient {
        val requests = mutableListOf<NetworkRequest>()

        override suspend fun <T> executeRequest(
            request: NetworkRequest,
            block: suspend (response: NetworkResponse) -> T,
        ): T {
            requests += request
            return block(NetworkResponse(code = responseCode))
        }
    }
}
