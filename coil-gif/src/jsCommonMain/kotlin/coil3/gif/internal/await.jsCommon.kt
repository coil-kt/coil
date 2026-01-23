package coil3.gif.internal

// Cannot block on JS - return null and the caller will skip the frame
internal actual fun <T> awaitFrame(block: suspend () -> T): T? = null
