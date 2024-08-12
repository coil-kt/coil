package coil3.network

import coil3.annotation.ExperimentalCoilApi
import coil3.network.internal.append
import okio.BufferedSink
import okio.BufferedSource

@ExperimentalCoilApi
object CacheResponse {

    fun readFrom(metadata: BufferedSource): NetworkResponse {
        val code = metadata.readUtf8LineStrict().toInt()
        val requestMillis = metadata.readUtf8LineStrict().toLong()
        val responseMillis = metadata.readUtf8LineStrict().toLong()
        val headers = NetworkHeaders.Builder()
        val headersLineCount = metadata.readUtf8LineStrict().toInt()
        for (i in 0 until headersLineCount) {
            headers.append(metadata.readUtf8LineStrict())
        }
        return NetworkResponse(
            code = code,
            requestMillis = requestMillis,
            responseMillis = responseMillis,
            headers = headers.build(),
        )
    }

    fun writeTo(response: NetworkResponse, sink: BufferedSink) {
        sink.writeInt(response.code).writeByte('\n'.code)
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
