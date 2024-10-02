package coil3.network

import coil3.network.internal.append
import okio.BufferedSink
import okio.BufferedSource

/**
 * Utility methods used to read/write a [NetworkResponse] from/to the disk cache.
 */
internal object CacheNetworkResponse {

    fun readFrom(source: BufferedSource): NetworkResponse {
        val code = source.readUtf8LineStrict().toInt()
        val requestMillis = source.readUtf8LineStrict().toLong()
        val responseMillis = source.readUtf8LineStrict().toLong()
        val headers = NetworkHeaders.Builder()
        val headersLineCount = source.readUtf8LineStrict().toInt()
        for (i in 0 until headersLineCount) {
            headers.append(source.readUtf8LineStrict())
        }
        return NetworkResponse(
            code = code,
            requestMillis = requestMillis,
            responseMillis = responseMillis,
            headers = headers.build(),
        )
    }

    fun writeTo(response: NetworkResponse, sink: BufferedSink) {
        sink.writeDecimalLong(response.code.toLong()).writeByte('\n'.code)
        sink.writeDecimalLong(response.requestMillis).writeByte('\n'.code)
        sink.writeDecimalLong(response.responseMillis).writeByte('\n'.code)
        val headers = response.headers.asMap().entries
        val headersLineCount = headers.sumOf { it.value.size }.toLong()
        sink.writeDecimalLong(headersLineCount).writeByte('\n'.code)
        for (header in headers) {
            for (value in header.value) {
                sink.writeUtf8(header.key)
                    .writeUtf8(":")
                    .writeUtf8(value)
                    .writeByte('\n'.code)
            }
        }
    }
}
