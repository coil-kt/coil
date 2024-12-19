package coil3.util

import coil3.annotation.InternalCoilApi
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable

/**
 * A [CoroutineDispatcher] that does not dispatch to [delegate] while [dispatchEnabled] is false.
 */
@InternalCoilApi
class DelayedDispatchCoroutineDispatcher(
    private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher() {
    var dispatchEnabled = false

    private val currentDispatcher: CoroutineDispatcher
        get() = if (dispatchEnabled) delegate else Dispatchers.Unconfined

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        // Enable dispatching when the context's dispatcher changes and it's not Dispatchers.Unconfined.
        dispatchEnabled = dispatchEnabled || context.dispatcher.let {
            it != null && it != this && it != Dispatchers.Unconfined
        }
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
