package coil.compose.utils

import androidx.annotation.RawRes
import androidx.test.platform.app.InstrumentationRegistry
import coil.compose.base.test.R
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import java.util.concurrent.TimeUnit

/**
 * A [MockWebServer] which returns a valid image responses at various paths, and a 404
 * for anything else.
 *
 * @param responseDelayMillis Allows the setting of a response delay to simulate 'real-world'
 * network conditions. Defaults to 0ms.
 */
@Suppress("TestFunctionName")
fun ImageMockWebServer(responseDelayMillis: Long = 0): MockWebServer {
    val dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest) = when (request.path) {
            "/image" -> {
                rawResourceAsResponse(
                    id = R.drawable.sample,
                    mimeType = "image/jpeg",
                ).setHeadersDelay(responseDelayMillis, TimeUnit.MILLISECONDS)
            }
            "/blue" -> {
                rawResourceAsResponse(
                    id = R.drawable.blue_rectangle,
                    mimeType = "image/png",
                ).setHeadersDelay(responseDelayMillis, TimeUnit.MILLISECONDS)
            }
            "/red" -> {
                rawResourceAsResponse(
                    id = R.drawable.red_rectangle,
                    mimeType = "image/png",
                ).setHeadersDelay(responseDelayMillis, TimeUnit.MILLISECONDS)
            }
            else -> {
                MockResponse()
                    .setHeadersDelay(responseDelayMillis, TimeUnit.MILLISECONDS)
                    .setResponseCode(404)
            }
        }
    }
    return MockWebServer().apply { setDispatcher(dispatcher) }
}

private fun rawResourceAsResponse(
    @RawRes id: Int,
    mimeType: String
): MockResponse {
    val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    return MockResponse()
        .addHeader("Content-Type", mimeType)
        .setBody(Buffer().apply { readFrom(resources.openRawResource(id)) })
}
