package coil.network

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readFully
import okio.BufferedSink

/** Modified from [Headers.Builder.add] */
internal fun Headers.Builder.addUnsafeNonAscii(line: String) = apply {
    val index = line.indexOf(':')
    require(index != -1) { "Unexpected header: $line" }
    addUnsafeNonAscii(line.substring(0, index).trim(), line.substring(index + 1))
}

internal fun Response.requireBody(): ResponseBody {
    return checkNotNull(body) { "response body == null" }
}

/** Write a [ByteReadChannel] to [sink] using streaming. */
internal suspend fun ByteReadChannel.readFully(sink: BufferedSink) {
    val buffer = ByteArray(OKIO_BUFFER_SIZE)

    while (!isClosedForRead) {
        val packet = readRemaining(buffer.size.toLong())
        if (packet.isEmpty) break

        val bytesRead = packet.remaining.toInt()
        packet.readFully(buffer, 0, bytesRead)
        sink.write(buffer, 0, bytesRead)
    }
}

// Okio uses 8 KB internally.
private const val OKIO_BUFFER_SIZE: Int = 8 * 1024
