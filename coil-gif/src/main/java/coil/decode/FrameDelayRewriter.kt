package coil.decode

import androidx.annotation.CheckResult
import okio.Buffer
import okio.BufferedSource
import okio.ByteString.Companion.decodeHex

/** Rewrite the frame delay in each graphics control block if it's below a threshold. */
internal class FrameDelayRewriter(private val isEnabled: Boolean) {

    @CheckResult
    fun rewriteFrameDelay(source: BufferedSource): BufferedSource {
        if (!isEnabled) return source

        source.use {
            // Search through the buffer and rewrite any frame delays below the threshold.
            val buffer = Buffer()
            while (true) {
                val index = source.indexOf(FRAME_DELAY_START_MARKER)
                if (index == -1L) break

                // Read up until the end of the frame delay start marker.
                source.read(buffer, index + FRAME_DELAY_START_MARKER.size)

                // Check for the end of the graphics control extension block.
                if (!source.request(1)) continue
                val size = source.buffer[0].toLong()
                if (size < 4 || !source.request(size) || source.buffer[size - 1] != 0.toByte()) continue

                // Rewrite the frame delay if it is below the threshold.
                if (source.buffer[2].toInt() < MINIMUM_FRAME_DELAY) {
                    buffer.writeByte(source.buffer[0].toInt())
                    buffer.writeByte(source.buffer[1].toInt())
                    buffer.writeByte(DEFAULT_FRAME_DELAY)
                    buffer.writeByte(0)
                    source.skip(4)
                }
            }
            source.readAll(buffer) // Read anything left in the source.
            return buffer
        }
    }

    private companion object {
        // https://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp
        // See: "Graphics Control Extension"
        private val FRAME_DELAY_START_MARKER = "0021F9".decodeHex()
        private const val MINIMUM_FRAME_DELAY = 2
        private const val DEFAULT_FRAME_DELAY = 10
    }
}
