package coil.network

import coil.util.createMockWebServer
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okio.Buffer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class CacheResponseTest {

    @Test
    fun `can serialize and deserialize cache response`() {
        val server = createMockWebServer(IMAGE)
        val url = server.url(IMAGE)
        val request = Request.Builder().url(url).build()
        val response = OkHttpClient().newCall(request).execute()
        val expected = CacheResponse(response)

        val buffer = Buffer()
        expected.writeTo(buffer)
        val actual = CacheResponse(buffer)

        assertEquals(expected.sentRequestAtMillis, actual.sentRequestAtMillis)
        assertEquals(expected.receivedResponseAtMillis, actual.receivedResponseAtMillis)
        assertEquals(expected.isTls, actual.isTls)
        assertEquals(expected.responseHeaders, actual.responseHeaders)
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1467 */
    @Test
    fun `can deserialize cache response with non ascii characters in headers`() {
        val headerName = "name"
        val headerValue = "微信图片"
        val server = createMockWebServer()
        val responseHeaders = Headers.Builder()
            .addUnsafeNonAscii(headerName, headerValue)
            .build()
        server.enqueue(MockResponse().setHeaders(responseHeaders).setBody(Buffer()))
        val url = server.url("微信图片.jpg")
        val request = Request.Builder().url(url).build()
        val response = OkHttpClient().newCall(request).execute()
        val expected = CacheResponse(response)

        val buffer = Buffer()
        expected.writeTo(buffer)
        val actual = CacheResponse(buffer)

        assertEquals(headerValue, actual.responseHeaders[headerName])
        assertEquals(expected.responseHeaders, actual.responseHeaders)
    }

    companion object {
        private const val IMAGE = "normal.jpg"
    }
}
