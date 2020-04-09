@file:Suppress("unused")

package coil.util

import android.util.Log
import coil.ImageLoader
import java.io.PrintWriter
import java.io.StringWriter

/**
 * A [Logger] implementation that writes to Android's [Log].
 *
 * NOTE: You **should not** enable this in release builds. Adding this to your [ImageLoader] reduces performance.
 * Additionally, this will log URLs which can contain [PII](https://en.wikipedia.org/wiki/Personal_data).
 */
class DebugLogger @JvmOverloads constructor(level: Int = Log.DEBUG) : Logger {

    override var level: Int = level
        set(value) {
            assertValidLevel(value)
            field = value
        }

    init {
        assertValidLevel(level)
    }

    override fun log(tag: String, priority: Int, message: String?, throwable: Throwable?) {
        if (message != null) {
            Log.println(priority, tag, message)
        }

        if (throwable != null) {
            val writer = StringWriter()
            throwable.printStackTrace(PrintWriter(writer))
            Log.println(priority, tag, writer.toString())
        }
    }

    private fun assertValidLevel(value: Int) {
        require(value in Log.VERBOSE..Log.ASSERT) { "Invalid log level: $value" }
    }
}
