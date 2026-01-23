package coil3.gif.internal

/**
 * Blocks the current thread until [block] completes.
 * Returns null on JS platforms where blocking is not possible.
 */
internal expect fun <T> awaitFrame(block: suspend () -> T): T?
