package coil.util

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Emojis for use in internal logging.
 *
 * TODO: Some emojis require an extra space to display correctly in logcat. Figure out why.
 */
internal object Emoji {
    const val BRAIN = "🧠" + " "
    const val FLOPPY = "💾"
    const val CLOUD = "☁️" + " "
    const val CONSTRUCTION = "🏗" + " "
    const val SIREN = "🚨"
}

internal inline fun log(tag: String, priority: Int, lazyMessage: () -> String) {
    if (CoilLogger.enabled && CoilLogger.level <= priority) {
        Log.println(priority, tag, lazyMessage())
    }
}

internal fun log(tag: String, throwable: Throwable) {
    if (CoilLogger.enabled && CoilLogger.level <= Log.ERROR) {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        Log.println(Log.ERROR, tag, writer.toString())
    }
}
