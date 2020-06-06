package coil.util

import android.util.Log
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
     *
     * @see Log
     */
    var level: Int

    /**
     * Write [message] and/or [throwable] to a logging destination.
     *
     * [priority] will be greater than or equal to [level].
     */
    fun log(tag: String, priority: Int, message: String?, throwable: Throwable?)
}
