package coil.util

import android.util.Log
import coil.util.Logger.Level
import java.io.PrintWriter
import java.io.StringWriter

/**
 * A [Logger] implementation that writes to Android's [Log].
 *
 * NOTE: You **should not** enable this in release builds. Adding this to your image loader reduces
 * performance. Additionally, this will log URLs which can contain
 * [PII](https://en.wikipedia.org/wiki/Personal_data).
 */
class DebugLogger @JvmOverloads constructor(
    override var minLevel: Level = Level.Debug
) : Logger {

    override fun log(tag: String, level: Level, message: String?, throwable: Throwable?) {
        if (message != null) {
            Log.println(level.toInt(), tag, message)
        }

        if (throwable != null) {
            val writer = StringWriter()
            throwable.printStackTrace(PrintWriter(writer))
            Log.println(level.toInt(), tag, writer.toString())
        }
    }

    private fun Level.toInt() = when (this) {
        Level.Verbose -> Log.VERBOSE
        Level.Debug -> Log.DEBUG
        Level.Info -> Log.INFO
        Level.Warn -> Log.WARN
        Level.Error -> Log.ERROR
    }
}
