package coil3.network.okhttp

import coil3.test.utils.context
import okhttp3.Headers
import okhttp3.Headers.Companion.headersOf
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.buffer
import okio.source

fun MockWebServer.enqueueImage(image: String, headers: Headers = headersOf()): Long {
    val buffer = Buffer()
    context.assets.open(image).source().buffer().readAll(buffer)
    enqueue(MockResponse().setHeaders(headers).setBody(buffer))
    return buffer.size
}
