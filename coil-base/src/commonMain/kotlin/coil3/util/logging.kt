package coil3.util

import coil3.ImageLoader
import kotlin.jvm.JvmOverloads

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
        Verbose, Debug, Info, Warn, Error,
    }
}

/**
 * A [Logger] implementation that writes to the platform's default logging mechanism.
 *
 * NOTE: You **should not** enable this in release builds. Adding this to your image loader
 * reduces performance. Additionally, this will log URLs which can contain personally identifiable
 * information.
 */
class DebugLogger @JvmOverloads constructor(
    override var minLevel: Logger.Level = Logger.Level.Debug,
) : Logger {

    override fun log(tag: String, level: Logger.Level, message: String?, throwable: Throwable?) {
        if (message != null) {
            println(level, tag, message)
        }

        if (throwable != null) {
            println(level, tag, throwable.stackTraceToString())
        }
    }
}
