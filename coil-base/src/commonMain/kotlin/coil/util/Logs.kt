package coil.util

import coil.util.Logger.Level

internal object Emoji {
    const val BRAIN = "🧠"
    const val FLOPPY = "💾"
    const val CLOUD = "☁️"
    const val CONSTRUCTION = "🏗"
    const val SIREN = "🚨"
}

internal inline fun Logger.log(tag: String, level: Level, lazyMessage: () -> String) {
    if (minLevel <= level) {
        log(tag, level, lazyMessage(), null)
    }
}

internal fun Logger.log(tag: String, throwable: Throwable) {
    if (minLevel <= Level.Error) {
        log(tag, Level.Error, null, throwable)
    }
}
