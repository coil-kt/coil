@file:Suppress("NOTHING_TO_INLINE")

package coil3.compose.internal

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A [CoroutineScope] that does not dispatch until the [CoroutineDispatcher] in its
 * [CoroutineContext] changes.
 */
internal fun DelayedDispatchCoroutineScope(
    context: CoroutineContext,
) = CoroutineScope(DelayedDispatchCoroutineContext(context))

/**
 * A special [CoroutineContext] implementation that automatically enables
 * [DelayedDispatchCoroutineDispatcher] dispatching if the context's [CoroutineDispatcher] changes.
 */
internal class DelayedDispatchCoroutineContext(
    context: CoroutineContext,
    val originalDispatcher: CoroutineDispatcher = context.dispatcher ?: Dispatchers.Unconfined,
) : ForwardingCoroutineContext(context) {

    override fun newContext(
        old: CoroutineContext,
        new: CoroutineContext,
    ): ForwardingCoroutineContext {
        val oldDispatcher = old.dispatcher
        val newDispatcher = new.dispatcher
        if (oldDispatcher is DelayedDispatchCoroutineDispatcher && oldDispatcher != newDispatcher) {
            oldDispatcher.unconfined = oldDispatcher.unconfined &&
                (newDispatcher == null || newDispatcher == Dispatchers.Unconfined)
        }

        return DelayedDispatchCoroutineContext(new, originalDispatcher)
    }
}

/** Launch [block] without dispatching. */
internal inline fun CoroutineScope.launchUndispatched(
    noinline block: suspend CoroutineScope.() -> Unit,
) = launch(Dispatchers.Unconfined, CoroutineStart.UNDISPATCHED, block)

/**
 * Execute [block] without dispatching until the scope's [CoroutineDispatcher] changes.
 * Must be called from inside a [DelayedDispatchCoroutineScope].
 */
internal suspend inline fun <T> withDelayedDispatch(
    noinline block: suspend CoroutineScope.() -> T,
): T {
    val originalDispatcher = (coroutineContext as DelayedDispatchCoroutineContext).originalDispatcher
    return withContext(DelayedDispatchCoroutineDispatcher(originalDispatcher), block)
}

/**
 * A [CoroutineDispatcher] that delegates to [Dispatchers.Unconfined] while [unconfined] is true
 * and [delegate] when [unconfined] is false.
 */
internal class DelayedDispatchCoroutineDispatcher(
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
        return "DelayedDispatchCoroutineDispatcher(delegate=$delegate)"
    }
}
