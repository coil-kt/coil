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
            val buffer = Buffer()
            var index = 0L

            // Search through the buffer and rewrite any frame delays below the threshold.
            while (true) {
                val frameDelayStartMarkerIndex = source.indexOf(FRAME_DELAY_START_MARKER, index)
                if (frameDelayStartMarkerIndex == -1L) break

                // Read up until the end of the frame delay start marker.
                index = frameDelayStartMarkerIndex + FRAME_DELAY_START_MARKER.size
                source.read(buffer, index - buffer.size)

                // Check that the frame delay end marker is present, else this is a false positive.
                if (!source.request(5) || source.buffer[4].toInt() != 0) continue

                // Rewrite the frame delay if it is below the threshold.
                if (source.buffer[2].toInt() < MINIMUM_FRAME_DELAY) {
                    buffer.writeByte(source.buffer[0].toInt())
                    buffer.writeByte(0)
                    buffer.writeByte(DEFAULT_FRAME_DELAY)
                    source.skip(3)
                    index += 3
                }
            }

            // Write the rest of the source and return the buffer.
            return buffer.apply { source.readAll(this) }
        }
    }

    private companion object {
        // The Graphics Control Extension block is guaranteed to match the following hexadecimal sequence:
        // 00 21 F9 04 XX FD FD XX 00
        // - FD is the frame delay value
        // - XX matches any byte value
        // https://www.matthewflickinger.com/lab/whatsinagif/images/graphic_control_ext.gif
        private val FRAME_DELAY_START_MARKER = "0021F904".decodeHex()
        private const val MINIMUM_FRAME_DELAY = 2
        private const val DEFAULT_FRAME_DELAY = 10
    }
}
