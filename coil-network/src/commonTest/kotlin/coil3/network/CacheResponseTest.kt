package coil3.network

import coil3.test.runTestAsync
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.request
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.util.date.GMTDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Job
import okio.Buffer

class CacheResponseTest {

    @Test
    fun canSerializeAndDeserializeCacheResponse() = runTestAsync {
        val response = HttpResponseData(
            statusCode = HttpStatusCode.OK,
            requestTime = GMTDate(1701673257L),
            headers = headers {
                append("name1", "value1")
                appendAll("name2", listOf("value2", "value3"))
            },
            version = HttpProtocolVersion.HTTP_2_0,
            body = "",
            callContext = Job(),
        )
        val config = MockEngineConfig()
        config.addHandler { response }
        val httpClient = HttpClient(MockEngine(config))
        val expected = CacheResponse(httpClient.request())

        val buffer = Buffer()
        expected.writeTo(buffer)
        val actual = CacheResponse(buffer)

        assertEquals(response.requestTime.timestamp, expected.sentRequestAtMillis)
        assertEquals(response.responseTime.timestamp, expected.receivedResponseAtMillis)
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
        val config = MockEngineConfig()
        config.addHandler { _ ->
            HttpResponseData(
                statusCode = HttpStatusCode.OK,
                requestTime = GMTDate(1701673257L),
                headers = headers {
                    set(headerName, headerValue)
                },
                version = HttpProtocolVersion.HTTP_2_0,
                body = "",
                callContext = Job(),
            )
        }
        val httpClient = HttpClient(MockEngine(config))
        val expected = CacheResponse(httpClient.request())

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
