package coil3.network.httpengine

import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkRequestBody
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import okio.ByteString.Companion.encodeUtf8

class HttpEngineRedirectPolicyTest {

    private val policy = HttpEngineRedirectPolicy.DEFAULT
    private val url = "https://example.com/images/start"

    @Test
    fun sensitiveHeadersCanFollowSameOriginRedirects() {
        val request = requestWithHeader("Authorization")

        assertTrue(policy.shouldFollowRedirect(request, url, "../image.png"))
        assertTrue(policy.shouldFollowRedirect(request, url, "https://EXAMPLE.com:443/image.png"))
    }

    @Test
    fun crossOriginRedirectsAllowOnlySafeHeadersAndNoBody() {
        val destination = "https://other.example.com/image.png"
        assertTrue(policy.shouldFollowRedirect(requestWithHeader("Accept"), url, destination))

        assertFalse(policy.shouldFollowRedirect(requestWithHeader("X-Token"), url, destination))
        val request = NetworkRequest(url, body = NetworkRequestBody("data".encodeUtf8()))
        assertFalse(policy.shouldFollowRedirect(request, url, destination))
    }

    @Test
    fun malformedRedirectsAreRejectedEvenWithoutSensitiveData() {
        assertFalse(policy.shouldFollowRedirect(NetworkRequest(url), url, "https://[invalid"))
    }

    private fun requestWithHeader(name: String): NetworkRequest {
        val headers = NetworkHeaders.Builder().set(name, "value").build()
        return NetworkRequest(url, headers = headers)
    }
}
