@file:Suppress("NOTHING_TO_INLINE")

package coil3.compose.internal

import kotlin.coroutines.CoroutineContext
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch

/**
 * A [CoroutineScope] that does not dispatch until the [CoroutineDispatcher] in its
 * [CoroutineContext] changes.
 */
internal fun DeferredDispatchCoroutineScope(
    context: CoroutineContext,
): CoroutineScope {
    val originalDispatcher = context.dispatcher ?: Dispatchers.Unconfined
    return CoroutineScope(DeferredDispatchCoroutineContext(context, originalDispatcher))
}

/**
 * A special [CoroutineContext] implementation that automatically enables
 * [DeferredDispatchCoroutineDispatcher] dispatching if the context's [CoroutineDispatcher] changes.
 */
internal class DeferredDispatchCoroutineContext(
    context: CoroutineContext,
    val originalDispatcher: CoroutineDispatcher,
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

        return DeferredDispatchCoroutineContext(new, originalDispatcher)
    }
}

/** Launch [block] without dispatching. */
internal inline fun CoroutineScope.launchUndispatched(
    noinline block: suspend CoroutineScope.() -> Unit,
) = launch(DeferredDispatchCoroutineDispatcher(Dispatchers.Unconfined), CoroutineStart.UNDISPATCHED, block)

/**
 * A [CoroutineDispatcher] that delegates to [Dispatchers.Unconfined] while [unconfined] is true
 * and [delegate] when [unconfined] is false.
 */
internal class DeferredDispatchCoroutineDispatcher(
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
