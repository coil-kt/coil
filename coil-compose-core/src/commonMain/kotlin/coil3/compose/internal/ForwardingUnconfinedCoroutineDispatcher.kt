package coil3.compose.internal

import coil3.util.Unconfined
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.withContext

internal suspend inline fun <T> withForwardingUnconfinedDispatcher(
    delegate: CoroutineDispatcher,
    noinline block: suspend CoroutineScope.() -> T,
) {
    val forwardingDispatcher = ForwardingUnconfinedCoroutineDispatcher(delegate)
    forwardingDispatcher.unconfined = true
    withContext(forwardingDispatcher, block)
    forwardingDispatcher.unconfined = true
}

/**
 * A [CoroutineDispatcher] that delegates to [Dispatchers.Unconfined] while [unconfined] is true
 * and [delegate] when [unconfined] is false.
 */
internal class ForwardingUnconfinedCoroutineDispatcher(
    private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher(), Unconfined {

    override var unconfined = false

    private val currentDispatcher: CoroutineDispatcher
        get() = if (unconfined) Dispatchers.Unconfined else delegate

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        println("isDispatchNeeded $currentDispatcher")
        return currentDispatcher.isDispatchNeeded(context)
    }

    override fun limitedParallelism(parallelism: Int, name: String?): CoroutineDispatcher {
        return currentDispatcher.limitedParallelism(parallelism, name)
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        currentDispatcher.dispatch(context, block)
    }

    @InternalCoroutinesApi
    override fun dispatchYield(context: CoroutineContext, block: Runnable) {
        currentDispatcher.dispatchYield(context, block)
    }

    override fun toString(): String {
        return "ForwardingUnconfinedCoroutineDispatcher(delegate=$delegate)"
    }
}
