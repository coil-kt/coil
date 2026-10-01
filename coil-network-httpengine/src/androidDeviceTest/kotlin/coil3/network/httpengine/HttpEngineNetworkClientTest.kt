package coil3.network.httpengine

import android.net.http.HttpEngine
import androidx.test.filters.SdkSuppress
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkRequestBody
import coil3.test.utils.context
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.ByteString.Companion.toByteString

@SdkSuppress(minSdkVersion = 34)
class HttpEngineNetworkClientTest {

    @Test
    fun responseBodyIsRead() = runTest {
        val server = MockWebServer()
        val executor = Executors.newSingleThreadExecutor()
        val engine = HttpEngine.Builder(context).build()
        try {
            val expectedBody = ByteArray(32 * 1024) { it.toByte() }
            server.enqueue(MockResponse().setBody(expectedBody.contentToString()))
            server.start()

            val client = HttpEngineNetworkClient(engine, executor)
            val request = NetworkRequest(server.url("/").toString())

            val response = client.executeRequest(request) {
                val buffer = Buffer()
                it.body!!.writeTo(buffer)
                buffer.readByteArray()
            }

            assertContentEquals(expectedBody, response)
        } finally {
            engine.shutdown()
            executor.shutdownNow()
            server.shutdown()
        }
    }

    @Test
    fun requestBodyIsUploaded() = runTest {
        val server = MockWebServer()
        val executor = Executors.newSingleThreadExecutor()
        val engine = HttpEngine.Builder(context).build()
        try {
            server.enqueue(MockResponse().setBody("ok"))
            server.start()

            val requestBody = "request-body".encodeToByteArray()
            val request = NetworkRequest(
                url = server.url("/").toString(),
                method = "POST",
                headers = NetworkHeaders.Builder()
                    .set("Content-Type", "text/plain")
                    .build(),
                body = NetworkRequestBody(requestBody.toByteString()),
            )

            HttpEngineNetworkClient(engine, executor).executeRequest(request) {
                it.body!!.writeTo(Buffer())
            }

            val recordedRequest = server.takeRequest()
            assertEquals("POST", recordedRequest.method)
            assertEquals("text/plain", recordedRequest.getHeader("Content-Type"))
            assertContentEquals(requestBody, recordedRequest.body.readByteArray())
        } finally {
            engine.shutdown()
            executor.shutdownNow()
            server.shutdown()
        }
    }

    @Test
    fun requestBodyGetsDefaultContentType() = runTest {
        val server = MockWebServer()
        val executor = Executors.newSingleThreadExecutor()
        val engine = HttpEngine.Builder(context).build()
        try {
            server.enqueue(MockResponse().setBody("ok"))
            server.start()

            val requestBody = "request-body".encodeToByteArray()
            val request = NetworkRequest(
                url = server.url("/").toString(),
                method = "POST",
                body = NetworkRequestBody(requestBody.toByteString()),
            )

            HttpEngineNetworkClient(engine, executor).executeRequest(request) {
                it.body!!.writeTo(Buffer())
            }

            val recordedRequest = server.takeRequest()
            assertEquals("application/octet-stream", recordedRequest.getHeader("Content-Type"))
            assertContentEquals(requestBody, recordedRequest.body.readByteArray())
        } finally {
            engine.shutdown()
            executor.shutdownNow()
            server.shutdown()
        }
    }

    @Test
    fun requestBodyCanBeRewoundForRedirect() = runTest {
        val server = MockWebServer()
        val executor = Executors.newSingleThreadExecutor()
        val engine = HttpEngine.Builder(context).build()
        try {
            server.start()
            val redirectUrl = server.url("/final")
            server.enqueue(
                MockResponse()
                    .setResponseCode(307)
                    .setHeader("Location", redirectUrl),
            )
            server.enqueue(MockResponse().setBody("ok"))

            val requestBody = "redirect-body".encodeToByteArray()
            val request = NetworkRequest(
                url = server.url("/redirect").toString(),
                method = "POST",
                headers = NetworkHeaders.Builder()
                    .set("Content-Type", "text/plain")
                    .build(),
                body = NetworkRequestBody(requestBody.toByteString()),
            )

            HttpEngineNetworkClient(engine, executor).executeRequest(request) {
                val buffer = Buffer()
                it.body!!.writeTo(buffer)
                assertEquals("ok", buffer.readUtf8())
            }

            val firstRequest = server.takeRequest()
            val secondRequest = server.takeRequest()
            assertEquals("POST", firstRequest.method)
            assertEquals("POST", secondRequest.method)
            assertContentEquals(requestBody, firstRequest.body.readByteArray())
            assertContentEquals(requestBody, secondRequest.body.readByteArray())
        } finally {
            engine.shutdown()
            executor.shutdownNow()
            server.shutdown()
        }
    }
}
