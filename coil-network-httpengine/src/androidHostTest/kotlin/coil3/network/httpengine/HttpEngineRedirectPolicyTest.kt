package coil3.network.httpengine

import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkRequestBody
import kotlin.test.Test
import kotlin.test.assertFailsWith
import okio.ByteString.Companion.encodeUtf8
import okio.IOException

class HttpEngineRedirectPolicyTest {

    private val policy = HttpEngineRedirectPolicy.DEFAULT
    private val url = "https://example.com/images/start"

    @Test
    fun sensitiveHeadersCanFollowSameOriginRedirects() {
        val request = requestWithHeader("Authorization")

        policy.checkRedirect(request, url, "../image.png")
        policy.checkRedirect(request, url, "https://EXAMPLE.com:443/image.png")
    }

    @Test
    fun crossOriginRedirectsAllowOnlySafeHeadersAndNoBody() {
        val destination = "https://other.example.com/image.png"
        policy.checkRedirect(requestWithHeader("Accept"), url, destination)

        assertFailsWith<IOException> {
            policy.checkRedirect(requestWithHeader("X-Token"), url, destination)
        }
        assertFailsWith<IOException> {
            val request = NetworkRequest(url, body = NetworkRequestBody("data".encodeUtf8()))
            policy.checkRedirect(request, url, destination)
        }
    }

    @Test
    fun malformedRedirectsFailEvenWithoutSensitiveData() {
        assertFailsWith<IOException> {
            policy.checkRedirect(NetworkRequest(url), url, "https://[invalid")
        }
    }

    private fun requestWithHeader(name: String): NetworkRequest {
        val headers = NetworkHeaders.Builder().set(name, "value").build()
        return NetworkRequest(url, headers = headers)
    }
}
