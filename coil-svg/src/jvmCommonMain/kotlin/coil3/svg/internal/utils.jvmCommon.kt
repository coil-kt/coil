package coil3.svg.internal

import kotlin.coroutines.CoroutineContext

internal actual suspend inline fun <T> runInterruptible(
    context: CoroutineContext,
    noinline block: () -> T,
): T = kotlinx.coroutines.runInterruptible(context, block)
