package coil.disk

import okio.Buffer
import okio.ForwardingSink
import okio.IOException
import okio.Sink

/** A sink that never throws [IOException]s, even if the underlying sink does. */
internal class FaultHidingSink(
    delegate: Sink,
    private val onException: (IOException) -> Unit
) : ForwardingSink(delegate) {

    private var hasErrors = false

    override fun write(source: Buffer, byteCount: Long) {
        if (hasErrors) {
            source.skip(byteCount)
            return
        }
        try {
            super.write(source, byteCount)
        } catch (e: IOException) {
            hasErrors = true
            onException(e)
        }
    }

    override fun flush() {
        try {
            super.flush()
        } catch (e: IOException) {
            hasErrors = true
            onException(e)
        }
    }

    override fun close() {
        try {
            super.close()
        } catch (e: IOException) {
            hasErrors = true
            onException(e)
        }
    }
}
