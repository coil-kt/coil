package coil3.network

import coil3.test.utils.runTestAsync
import kotlin.test.Test
import kotlin.test.assertEquals
import okio.Buffer

class CacheResponseTest {

    @Test
    fun canSerializeAndDeserializeCacheResponse() = runTestAsync {
        val response = NetworkResponse(
            request = NetworkRequest("https://example.com/image.jpg"),
            requestMillis = 1701673257L,
            responseMillis = 1701674491L,
            headers = NetworkHeaders.Builder()
                .set("name1", "value1")
                .set("name2", listOf("value2", "value3"))
                .build(),
        )
        val expected = CacheResponse(response)

        val buffer = Buffer()
        expected.writeTo(buffer)
        val actual = CacheResponse(buffer)

        assertEquals(response.requestMillis, expected.sentRequestAtMillis)
        assertEquals(response.responseMillis, expected.receivedResponseAtMillis)
        assertEquals(response.headers, expected.responseHeaders)
        assertEquals(expected.sentRequestAtMillis, actual.sentRequestAtMillis)
        assertEquals(expected.receivedResponseAtMillis, actual.receivedResponseAtMillis)
        assertEquals(expected.responseHeaders, actual.responseHeaders)
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1467 */
    @Test
    fun canDeserializeCacheResponseWithNonAsciiCharactersInHeaders() = runTestAsync {
        val headerName = "name"
        val headerValue = "微信图片"
        val response = NetworkResponse(
            request = NetworkRequest("https://example.com/image.jpg"),
            requestMillis = 1701673257L,
            responseMillis = 1701674491L,
            headers = NetworkHeaders.Builder()
                .set(headerName, headerValue)
                .build(),
        )
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
