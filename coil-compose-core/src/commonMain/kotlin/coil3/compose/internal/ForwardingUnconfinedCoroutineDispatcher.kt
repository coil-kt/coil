package coil3.compose.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import coil3.util.Unconfined
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable

/**
 * Create a [CoroutineScope] that will contain a [ForwardingUnconfinedCoroutineDispatcher] if necessary.
 */
@Composable
internal fun rememberForwardingUnconfinedCoroutineScope(): CoroutineScope {
    val scope = rememberCoroutineScope()
    return remember(scope) {
        val currentContext = scope.coroutineContext
        val currentDispatcher = currentContext.dispatcher
        if (currentDispatcher != null && currentDispatcher != Dispatchers.Unconfined) {
            CoroutineScope(currentContext + ForwardingUnconfinedCoroutineDispatcher(currentDispatcher))
        } else {
            scope
        }
    }
}

/**
 * A [CoroutineDispatcher] that delegates to [Dispatchers.Unconfined] while [unconfined] is true
 * and [delegate] when [unconfined] is false.
 */
internal class ForwardingUnconfinedCoroutineDispatcher(
    private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher(), Unconfined {

    override var unconfined = true

    private val currentDispatcher: CoroutineDispatcher
        get() = if (unconfined) Dispatchers.Unconfined else delegate

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
