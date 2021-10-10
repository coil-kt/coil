package coil.util

/** A simple wrapper for [System.currentTimeMillis] to support testing. */
internal object Time {

    private var provider: () -> Long = System::currentTimeMillis

    fun currentMillis(): Long = provider()

    fun setCurrentMillis(currentMillis: Long) {
        provider = { currentMillis }
    }

    fun reset() {
        provider = System::currentTimeMillis
    }
}
