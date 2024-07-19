package coil3.network.ktor3.internal

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.readFully
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.readRemaining
import okio.BufferedSink
import okio.FileSystem
import okio.Path

internal actual suspend fun ByteReadChannel.writeTo(sink: BufferedSink) {
    val buffer = ByteArray(OKIO_BUFFER_SIZE)

    while (!isClosedForRead) {
        val packet = readRemaining(buffer.size.toLong())
        if (packet.exhausted()) break

        // TODO: Figure out how to remove 'buffer' and read directly into 'sink'.
        val bytesRead = packet.remaining.toInt()
        packet.readFully(buffer, 0, bytesRead)
        sink.write(buffer, 0, bytesRead)
    }

    closedCause?.let { throw it }
}

internal actual suspend fun ByteReadChannel.writeTo(fileSystem: FileSystem, path: Path) {
    fileSystem.write(path) {
        writeTo(this)
    }
}

// Okio uses 8 KB internally.
private const val OKIO_BUFFER_SIZE = 8 * 1024
