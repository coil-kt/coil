package coil3.gif.internal

import kotlinx.coroutines.runBlocking

internal actual fun <T> awaitFrame(block: suspend () -> T): T? = runBlocking { block() }
