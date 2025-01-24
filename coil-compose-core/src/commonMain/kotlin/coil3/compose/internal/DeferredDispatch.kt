package coil3.compose.internal

import kotlin.coroutines.CoroutineContext
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch

/**
 * Launch [block] and defer dispatching until the context's [CoroutineDispatcher] changes.
 */
internal fun CoroutineScope.launchWithDeferredDispatch(
    block: suspend CoroutineScope.() -> Unit,
): Job = CoroutineScope(DeferredDispatchCoroutineContext(coroutineContext)).launch(
    context = DeferredDispatchCoroutineDispatcher(
        delegate = coroutineContext.dispatcher ?: Dispatchers.Unconfined,
    ),
    start = CoroutineStart.UNDISPATCHED,
    block = block,
)

/**
 * A special [CoroutineContext] implementation that automatically enables
 * [DeferredDispatchCoroutineDispatcher] dispatching if the context's [CoroutineDispatcher] changes.
 */
private class DeferredDispatchCoroutineContext(
    context: CoroutineContext,
) : ForwardingCoroutineContext(context) {

    override fun newContext(
        old: CoroutineContext,
        new: CoroutineContext,
    ): ForwardingCoroutineContext {
        val oldDispatcher = old.dispatcher
        val newDispatcher = new.dispatcher
        if (oldDispatcher is DeferredDispatchCoroutineDispatcher && oldDispatcher != newDispatcher) {
            oldDispatcher.unconfined = oldDispatcher.unconfined &&
                (newDispatcher == null || newDispatcher == Dispatchers.Unconfined)
        }

        return DeferredDispatchCoroutineContext(new)
    }
}

/**
 * A [CoroutineDispatcher] that delegates to [Dispatchers.Unconfined] while [unconfined] is true
 * and [delegate] when [unconfined] is false.
 */
private class DeferredDispatchCoroutineDispatcher(
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
        return "DeferredDispatchCoroutineDispatcher(delegate=$delegate)"
    }
}
