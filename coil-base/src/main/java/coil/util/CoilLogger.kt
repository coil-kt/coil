@file:Suppress("unused")

package coil.util

import android.util.Log

/** A singleton that enables/disables logging to [Log]. */
object CoilLogger {

    internal var enabled = false
        private set
    internal var level = Log.DEBUG
        private set

    /**
     * Enable/disable logging.
     *
     * NOTE: Enabling this reduces performance. Additionally, this will log URLs which can contain
     * [PII](https://en.wikipedia.org/wiki/Personal_data). You should **not** enable this in release builds.
     */
    @JvmStatic
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    /**
     * Set the minimum importance for Coil to log.
     *
     * @see Log
     */
    @JvmStatic
    fun setLevel(level: Int) {
        require(level in Log.VERBOSE..Log.ASSERT) { "Invalid log level." }
        this.level = level
    }
}
