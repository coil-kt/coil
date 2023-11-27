package coil.network

import coil.annotation.ExperimentalCoilApi
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.headers
import kotlin.LazyThreadSafetyMode.NONE
import okio.BufferedSink
import okio.BufferedSource

/** Holds the response metadata for an image in the disk cache. */
@ExperimentalCoilApi
class CacheResponse {
    val cacheControl: String? by lazy(NONE) { responseHeaders[CACHE_CONTROL] }
    val contentType: String? by lazy(NONE) { responseHeaders[CONTENT_TYPE] }
    val sentRequestAtMillis: Long
    val receivedResponseAtMillis: Long
    val responseHeaders: Headers

    constructor(source: BufferedSource) {
        this.sentRequestAtMillis = source.readUtf8LineStrict().toLong()
        this.receivedResponseAtMillis = source.readUtf8LineStrict().toLong()
        this.responseHeaders = headers {
            val responseHeadersLineCount = source.readUtf8LineStrict().toInt()
            for (i in 0 until responseHeadersLineCount) {
                append(source.readUtf8LineStrict())
            }
        }
    }

    constructor(response: HttpResponse, headers: Headers = response.headers) {
        this.sentRequestAtMillis = response.requestTime.timestamp
        this.receivedResponseAtMillis = response.responseTime.timestamp
        this.responseHeaders = headers
    }

    fun writeTo(sink: BufferedSink) {
        sink.writeDecimalLong(sentRequestAtMillis).writeByte('\n'.code)
        sink.writeDecimalLong(receivedResponseAtMillis).writeByte('\n'.code)
        val responseHeaders = responseHeaders.entries()
        val responseHeadersLineCount = responseHeaders.sumOf { it.value.size }.toLong()
        sink.writeDecimalLong(responseHeadersLineCount).writeByte('\n'.code)
        for (header in responseHeaders) {
            for (value in header.value) {
                sink.writeUtf8(header.key)
                    .writeUtf8(": ")
                    .writeUtf8(value)
                    .writeByte('\n'.code)
            }
        }
    }
}
