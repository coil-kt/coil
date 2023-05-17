package coil.util

import coil.ImageLoader

/**
 * Logging interface for [ImageLoader]s.
 *
 * @see ImageLoader.Builder.logger
 * @see DebugLogger
 */
interface Logger {

    /**
     * The minimum level for this logger to log.
     */
    var minLevel: Level

    /**
     * Write [message] and/or [throwable] to a logging destination.
     *
     * [level] will be greater than or equal to [level].
     */
    fun log(tag: String, level: Level, message: String?, throwable: Throwable?)

    /**
     * The priority level for a log message.
     */
    enum class Level {
        Verbose, Debug, Info, Warn, Error
    }
}
