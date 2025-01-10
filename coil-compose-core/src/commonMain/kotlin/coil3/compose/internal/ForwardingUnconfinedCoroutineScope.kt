package coil3.compose.internal

import kotlin.coroutines.CoroutineContext
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable

/**
 * A [CoroutineScope] with a special [CoroutineContext] that enables [ForwardingUnconfinedCoroutineDispatcher]
 * to enable dispatching after it is replaced in the context.
 */
internal fun ForwardingUnconfinedCoroutineScope(
    context: CoroutineContext,
) = CoroutineScope(
    context = ForwardingCoroutineContext(context) { old, new ->
        val oldDispatcher = old.dispatcher
        val newDispatcher = new.dispatcher
        if (oldDispatcher is ForwardingUnconfinedCoroutineDispatcher && oldDispatcher != newDispatcher) {
            oldDispatcher.unconfined = oldDispatcher.unconfined &&
                    (newDispatcher == null || newDispatcher == Dispatchers.Unconfined)
        }
    }
)

/**
 * A [CoroutineDispatcher] that delegates to [Dispatchers.Unconfined] while [unconfined] is true
 * and [delegate] when [unconfined] is false.
 */
internal class ForwardingUnconfinedCoroutineDispatcher(
    private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher() {
    private val _unconfined = atomic(true)
    var unconfined by _unconfined

    private val currentDispatcher: CoroutineDispatcher
        get() = if (_unconfined.value) Dispatchers.Unconfined else delegate

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
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
