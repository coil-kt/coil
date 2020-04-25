@file:JvmName("-Logs")
@file:Suppress("NOTHING_TO_INLINE")

package coil.util

import android.util.Log

/**
 * Emojis for use in internal logging.
 *
 * Some emojis require an extra space to display correctly in logcat. I'm not sure why. 🤷
 */
internal object Emoji {
    const val BRAIN = "🧠"
    const val FLOPPY = "💾"
    const val CLOUD = "☁️" + " "
    const val CONSTRUCTION = "🏗" + " "
    const val SIREN = "🚨"
}

internal inline fun Logger.log(tag: String, priority: Int, lazyMessage: () -> String) {
    if (level <= priority) {
        log(tag, priority, lazyMessage(), null)
    }
}

internal fun Logger.log(tag: String, throwable: Throwable) {
    if (level <= Log.ERROR) {
        log(tag, Log.ERROR, null, throwable)
    }
}
