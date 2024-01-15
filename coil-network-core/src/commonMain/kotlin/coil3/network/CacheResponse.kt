package coil3.network

import coil3.annotation.ExperimentalCoilApi
import coil3.network.internal.append
import okio.BufferedSink
import okio.BufferedSource

/** Holds the response metadata for an image in the disk cache. */
@ExperimentalCoilApi
class CacheResponse {
    val sentRequestAtMillis: Long
    val receivedResponseAtMillis: Long
    val responseHeaders: NetworkHeaders

    constructor(source: BufferedSource) {
        this.sentRequestAtMillis = source.readUtf8LineStrict().toLong()
        this.receivedResponseAtMillis = source.readUtf8LineStrict().toLong()
        val headers = NetworkHeaders.Builder()
        val responseHeadersLineCount = source.readUtf8LineStrict().toInt()
        for (i in 0 until responseHeadersLineCount) {
            headers.append(source.readUtf8LineStrict())
        }
        this.responseHeaders = headers.build()
    }

    constructor(response: NetworkResponse, headers: NetworkHeaders = response.headers) {
        this.sentRequestAtMillis = response.requestMillis
        this.receivedResponseAtMillis = response.responseMillis
        this.responseHeaders = headers
    }

    fun writeTo(sink: BufferedSink) {
        sink.writeDecimalLong(sentRequestAtMillis).writeByte('\n'.code)
        sink.writeDecimalLong(receivedResponseAtMillis).writeByte('\n'.code)
        val responseHeaders = responseHeaders.asMap().entries
        val responseHeadersLineCount = responseHeaders.sumOf { it.value.size }.toLong()
        sink.writeDecimalLong(responseHeadersLineCount).writeByte('\n'.code)
        for (header in responseHeaders) {
            for (value in header.value) {
                sink.writeUtf8(header.key)
                    .writeUtf8(":")
                    .writeUtf8(value)
                    .writeByte('\n'.code)
            }
        }
    }
}
