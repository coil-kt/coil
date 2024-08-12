package coil3.network

import coil3.annotation.ExperimentalCoilApi
import coil3.network.internal.append
import okio.BufferedSink
import okio.BufferedSource

@ExperimentalCoilApi
fun CacheResponse(source: BufferedSource): CacheResponse {
    val sentRequestAtMillis = source.readUtf8LineStrict().toLong()
    val receivedResponseAtMillis = source.readUtf8LineStrict().toLong()
    val headers = NetworkHeaders.Builder()
    val responseHeadersLineCount = source.readUtf8LineStrict().toInt()
    for (i in 0 until responseHeadersLineCount) {
        headers.append(source.readUtf8LineStrict())
    }
    return CacheResponse(
        sentRequestAtMillis = sentRequestAtMillis,
        receivedResponseAtMillis = receivedResponseAtMillis,
        responseHeaders = headers.build(),
    )
}

@ExperimentalCoilApi
fun CacheResponse(
    response: NetworkResponse,
    headers: NetworkHeaders = response.headers,
) = CacheResponse(
    sentRequestAtMillis = response.requestMillis,
    receivedResponseAtMillis = response.responseMillis,
    responseHeaders = headers,
)

/**
 * Holds the response metadata for an image in the disk cache.
 */
@ExperimentalCoilApi
class CacheResponse(
    val sentRequestAtMillis: Long,
    val receivedResponseAtMillis: Long,
    val responseHeaders: NetworkHeaders,
) {

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

    fun copy(
        sentRequestAtMillis: Long = this.sentRequestAtMillis,
        receivedResponseAtMillis: Long = this.receivedResponseAtMillis,
        responseHeaders: NetworkHeaders = this.responseHeaders,
    ) = CacheResponse(
        sentRequestAtMillis = sentRequestAtMillis,
        receivedResponseAtMillis = receivedResponseAtMillis,
        responseHeaders = responseHeaders,
    )
}
