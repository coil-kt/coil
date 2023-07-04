package coil.network

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readFully
import okio.Sink
import okio.Source
import okio.buffer
import okio.use

internal suspend fun ByteReadChannel.readFully(sink: Sink) {
    sink.buffer().use { bufferedSink ->
        val buffer = ByteArray(OKIO_BUFFER_SIZE)

        while (!isClosedForRead) {
            val packet = readRemaining(buffer.size.toLong())
            if (packet.isEmpty) break

            val bytesRead = packet.remaining.toInt()
            packet.readFully(buffer, 0, bytesRead)
            bufferedSink.write(buffer, 0, bytesRead)
        }
    }
}

internal suspend fun ByteWriteChannel.writeAll(source: Source) {
    source.buffer().use { bufferedSource ->
        val buffer = ByteArray(OKIO_BUFFER_SIZE)

        while (!isClosedForWrite) {
            val bytesRead = bufferedSource.read(buffer)
            if (bytesRead == -1) break

            writeFully(buffer, 0, bytesRead)
            flush()
        }
    }
}

// Okio uses 8 KB internally.
private const val OKIO_BUFFER_SIZE: Int = 8 * 1024
