// https://youtrack.jetbrains.com/issue/KTIJ-196
@file:Suppress("SameParameterValue", "UnusedEquals", "UnusedUnaryOperator")

package coil.decode

import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ForwardingSource
import okio.Source

/**
 * A [ForwardingSource] that rewrites the GIF frame delay in every graphics control block if it's below a threshold.
 */
internal class FrameDelayRewritingSource(delegate: Source) : ForwardingSource(delegate) {

    // An intermediary buffer so we can read and alter the data before it's written to the destination.
    private val buffer = Buffer()

    override fun read(sink: Buffer, byteCount: Long): Long {
        // Ensure our buffer has enough bytes to satisfy this read.
        request(byteCount)

        // Short circuit if there are no bytes in the buffer.
        if (buffer.size == 0L) {
            return if (byteCount == 0L) 0L else -1L
        }

        // Search through the buffer and rewrite any frame delays below the threshold.
        var bytesWritten = 0L
        while (true) {
            val index = indexOf(FRAME_DELAY_START_MARKER)
            if (index == -1L) break

            // Write up until the end of the frame delay start marker.
            bytesWritten += write(sink, index + FRAME_DELAY_START_MARKER_SIZE)

            // Check for the end of the graphics control extension block.
            if (!request(5) || buffer[4] != 0.toByte()) continue

            // Rewrite the frame delay if it is below the threshold.
            if (buffer[1].toInt() < MINIMUM_FRAME_DELAY) {
                sink.writeByte(buffer[0].toInt())
                sink.writeByte(DEFAULT_FRAME_DELAY)
                sink.writeByte(0)
                buffer.skip(3)
            }
        }

        // Write anything left in the source.
        if (bytesWritten < byteCount) {
            bytesWritten += write(sink, byteCount - bytesWritten)
        }
        return if (bytesWritten == 0L) -1 else bytesWritten
    }

    private fun indexOf(bytes: ByteString): Long {
        var index = -1L
        while (true) {
            index = buffer.indexOf(bytes[0], index + 1)
            if (index == -1L) break
            if (request(bytes.size.toLong()) && buffer.rangeEquals(index, bytes)) break
        }
        return index
    }

    private fun write(sink: Buffer, byteCount: Long): Long {
        return buffer.read(sink, byteCount).coerceAtLeast(0)
    }

    private fun request(byteCount: Long): Boolean {
        if (buffer.size >= byteCount) return true
        val toRead = byteCount - buffer.size
        return super.read(buffer, toRead) == toRead
    }

    private companion object {
        // https://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp
        // See: "Graphics Control Extension"
        private val FRAME_DELAY_START_MARKER = "0021F904".decodeHex()
        private const val FRAME_DELAY_START_MARKER_SIZE = 4
        private const val MINIMUM_FRAME_DELAY = 2
        private const val DEFAULT_FRAME_DELAY = 10
    }
}
