package coil3.network.ktor3.internal

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.core.read
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.SelectableChannel
import java.nio.channels.WritableByteChannel
import okio.BufferedSink
import okio.FileSystem
import okio.Path

internal actual suspend fun ByteReadChannel.writeTo(sink: BufferedSink) {
    copyTo(sink)
}

internal actual suspend fun ByteReadChannel.writeTo(fileSystem: FileSystem, path: Path) {
    if (fileSystem === FileSystem.SYSTEM) {
        // Fast path: normal jvm File, write to FileChannel directly.
        RandomAccessFile(path.toFile(), "rw").use {
            copyTo(it.channel)
        }
    } else {
        // Slow path: cannot guarantee a "real" file.
        fileSystem.write(path) {
            copyTo(this)
        }
    }
}

/**
 * Copied from https://github.com/ktorio/ktor/blob/main/ktor-io/jvm/src/io/ktor/utils/io/ByteReadChannelOperations.jvm.kt
 * to work around https://youtrack.jetbrains.com/issue/KTOR-7220.
 */
@OptIn(InternalAPI::class)
private suspend fun ByteReadChannel.copyTo(
    channel: WritableByteChannel,
    limit: Long = Long.MAX_VALUE,
): Long {
    require(limit >= 0L) { "Limit shouldn't be negative: $limit" }
    if (channel is SelectableChannel && !channel.isBlocking) {
        throw IllegalArgumentException("Non-blocking channels are not supported")
    }

    if (isClosedForRead) {
        closedCause?.let { throw it }
        return 0
    }

    var copied = 0L
    val copy = { bb: ByteBuffer ->
        val rem = limit - copied

        if (rem < bb.remaining()) {
            val l = bb.limit()
            bb.limit(bb.position() + rem.toInt())

            while (bb.hasRemaining()) {
                channel.write(bb)
            }

            bb.limit(l)
            copied += rem
        } else {
            var written = 0L
            while (bb.hasRemaining()) {
                written += channel.write(bb)
            }

            copied += written
        }
    }

    while (copied < limit) {
        if (isClosedForRead || !awaitContent()) break
        if (readBuffer.request(1)) {
            readBuffer.read(copy)
        }
    }

    closedCause?.let { throw it }

    return copied
}
