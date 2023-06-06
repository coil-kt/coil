package coil.util

import coil.annotation.VisibleForTesting

/** A simple wrapper for [getTimeMillis] to support testing. */
internal object Time {
    private var provider: () -> Long = ::getTimeMillis

    fun currentMillis(): Long = provider()

    @VisibleForTesting
    fun setCurrentMillis(currentMillis: Long) {
        provider = { currentMillis }
    }

    @VisibleForTesting
    fun reset() {
        provider = ::getTimeMillis
    }
}

internal expect fun getTimeMillis(): Long
