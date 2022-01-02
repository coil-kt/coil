package coil.network

import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Response
import okio.BufferedSink
import okio.BufferedSource
import kotlin.LazyThreadSafetyMode.NONE

/** Holds the response metadata for an image in the disk cache. */
internal class CacheResponse {

    val cacheControl by lazy(NONE) { CacheControl.parse(responseHeaders) }
    val contentType by lazy(NONE) { responseHeaders["Content-Type"]?.toMediaTypeOrNull() }
    val sentRequestAtMillis: Long
    val receivedResponseAtMillis: Long
    val isTls: Boolean
    val responseHeaders: Headers

    constructor(source: BufferedSource) {
        this.sentRequestAtMillis = source.readUtf8LineStrict().toLong()
        this.receivedResponseAtMillis = source.readUtf8LineStrict().toLong()
        this.isTls = source.readUtf8LineStrict().toInt() > 0
        val responseHeadersLineCount = source.readUtf8LineStrict().toInt()
        val responseHeaders = Headers.Builder()
        for (i in 0 until responseHeadersLineCount) {
            responseHeaders.add(source.readUtf8LineStrict())
        }
        this.responseHeaders = responseHeaders.build()
    }

    constructor(response: Response) {
        this.sentRequestAtMillis = response.sentRequestAtMillis
        this.receivedResponseAtMillis = response.receivedResponseAtMillis
        this.isTls = response.handshake != null
        this.responseHeaders = response.headers
    }

    fun writeTo(sink: BufferedSink) {
        sink.writeDecimalLong(sentRequestAtMillis).writeByte('\n'.code)
        sink.writeDecimalLong(receivedResponseAtMillis).writeByte('\n'.code)
        sink.writeDecimalLong(if (isTls) 1L else 0L).writeByte('\n'.code)
        sink.writeDecimalLong(responseHeaders.size.toLong()).writeByte('\n'.code)
        for (i in 0 until responseHeaders.size) {
            sink.writeUtf8(responseHeaders.name(i))
                .writeUtf8(": ")
                .writeUtf8(responseHeaders.value(i))
                .writeByte('\n'.code)
        }
    }
}
