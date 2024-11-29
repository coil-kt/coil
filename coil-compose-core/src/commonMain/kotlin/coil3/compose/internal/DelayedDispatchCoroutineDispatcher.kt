package coil3.compose.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable

/**
 * Create a [CoroutineScope] will contain a [DelayedDispatchCoroutineDispatcher] if necessary.
 */
@Composable
internal fun rememberDelayedDispatchCoroutineScope(): CoroutineScope {
    val scope = rememberCoroutineScope()
    return remember(scope) {
        val currentContext = scope.coroutineContext
        val currentDispatcher = scope.coroutineContext.dispatcher
        if (currentDispatcher != null && currentDispatcher != Dispatchers.Unconfined) {
            CoroutineScope(currentContext + DelayedDispatchCoroutineDispatcher(currentDispatcher))
        } else {
            scope
        }
    }
}

/**
 * A [CoroutineDispatcher] that delays dispatching to [delegate] until after the current
 * [CoroutineDispatcher] changes.
 */
private class DelayedDispatchCoroutineDispatcher(
    private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher() {
    private var dispatchEnabled = false

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
