package coil3.network.ktor.internal

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readFully
import okio.BufferedSink
import okio.FileSystem
import okio.Path

internal actual suspend fun ByteReadChannel.writeTo(sink: BufferedSink) {
    val buffer = ByteArray(OKIO_BUFFER_SIZE)

    while (!isClosedForRead) {
        val packet = readRemaining(buffer.size.toLong())
        if (packet.isEmpty) break

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
