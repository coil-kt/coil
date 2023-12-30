package coil3.util

import android.util.Log

internal actual fun println(level: Logger.Level, tag: String, message: String) {
    Log.println(level.toInt(), tag, message)
}

private fun Logger.Level.toInt() = when (this) {
    Logger.Level.Verbose -> Log.VERBOSE
    Logger.Level.Debug -> Log.DEBUG
    Logger.Level.Info -> Log.INFO
    Logger.Level.Warn -> Log.WARN
    Logger.Level.Error -> Log.ERROR
}
