package coil.util

/** A simple wrapper for [getTimeMillis] to support testing. */
internal object Time {
    private var provider: () -> Long = ::getTimeMillis

    fun currentMillis(): Long = provider()

    fun setCurrentMillis(currentMillis: Long) {
        provider = { currentMillis }
    }

    fun reset() {
        provider = ::getTimeMillis
    }
}

internal expect fun getTimeMillis(): Long
