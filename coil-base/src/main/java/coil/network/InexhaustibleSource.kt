package coil.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.Source
import java.nio.ByteBuffer

/** Wraps the [Response] body's source with [InexhaustibleSource]. */
internal class InexhaustibleSourceInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val body = response.body ?: return response
        val source = InexhaustibleSource(body.source())
        return response.newBuilder()
            .body(object : ResponseBody() {
                override fun source() = source
                override fun contentType() = body.contentType()
                override fun contentLength() = body.contentLength()
                override fun close() = body.close()
            })
            .build()
    }
}

/** Wraps [delegate] to prevent it from returning -1 from all [Source.read] overloads. */
internal class InexhaustibleSource(
    private val delegate: BufferedSource
) : BufferedSource by delegate {

    var isEnabled = false
    var isExhausted = false
        private set

    override fun read(sink: Buffer, byteCount: Long) =
        interceptBytesRead(delegate.read(sink, byteCount))

    override fun read(sink: ByteArray) = interceptBytesRead(delegate.read(sink))

    override fun read(sink: ByteArray, offset: Int, byteCount: Int) =
        interceptBytesRead(delegate.read(sink, offset, byteCount))

    override fun read(buffer: ByteBuffer) = interceptBytesRead(delegate.read(buffer))

    private fun interceptBytesRead(bytesRead: Int): Int =
        interceptBytesRead(bytesRead.toLong()).toInt()

    private fun interceptBytesRead(bytesRead: Long): Long {
        if (bytesRead == -1L) {
            isExhausted = true
            if (isEnabled) return 0L
        }
        return bytesRead
    }
}
