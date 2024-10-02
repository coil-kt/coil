package coil3.network

import coil3.test.utils.runTestAsync
import kotlin.test.Test
import kotlin.test.assertEquals
import okio.Buffer

class CacheResponseTest {

    @Test
    fun canSerializeAndDeserializeCacheResponse() = runTestAsync {
        val expected = NetworkResponse(
            requestMillis = 1701673257L,
            responseMillis = 1701674491L,
            headers = NetworkHeaders.Builder()
                .set("name1", "value1")
                .set("name2", listOf("value2", "value3"))
                .build(),
        )

        val buffer = Buffer()
        CacheNetworkResponse.writeTo(expected, buffer)
        val actual = CacheNetworkResponse.readFrom(buffer)

        assertEquals(expected.requestMillis, actual.requestMillis)
        assertEquals(expected.responseMillis, actual.responseMillis)
        assertEquals(expected.headers, actual.headers)
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1467 */
    @Test
    fun canDeserializeCacheResponseWithNonAsciiCharactersInHeaders() = runTestAsync {
        val headerName = "name"
        val headerValue = "微信图片"
        val expected = NetworkResponse(
            requestMillis = 1701673257L,
            responseMillis = 1701674491L,
            headers = NetworkHeaders.Builder()
                .set(headerName, headerValue)
                .build(),
        )
        val buffer = Buffer()
        CacheNetworkResponse.writeTo(expected, buffer)
        val actual = CacheNetworkResponse.readFrom(buffer)

        assertEquals(headerValue, actual.headers[headerName])
        assertEquals(expected.headers, actual.headers)
    }

    companion object {
        private const val IMAGE = "normal.jpg"
    }
}
