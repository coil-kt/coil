package coil3.svg.internal

import kotlin.coroutines.CoroutineContext

internal actual suspend inline fun <T> runInterruptible(
    context: CoroutineContext,
    block: () -> T,
): T = block()
