package coil3.disk

import okio.Buffer
import okio.IOException
import okio.Sink

/** A sink that never throws [IOException]s, even if the underlying sink does. */
internal class FaultHidingSink(
    private val delegate: Sink,
    private val onException: (IOException) -> Unit,
) : Sink by delegate {

    private var hasErrors = false

    override fun write(source: Buffer, byteCount: Long) {
        if (hasErrors) {
            source.skip(byteCount)
            return
        }
        try {
            delegate.write(source, byteCount)
        } catch (e: IOException) {
            hasErrors = true
            onException(e)
        }
    }

    override fun flush() {
        try {
            delegate.flush()
        } catch (e: IOException) {
            hasErrors = true
            onException(e)
        }
    }

    override fun close() {
        try {
            delegate.close()
        } catch (e: IOException) {
            hasErrors = true
            onException(e)
        }
    }
}
