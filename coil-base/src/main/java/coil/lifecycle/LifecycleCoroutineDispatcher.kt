package coil.lifecycle

import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import java.util.ArrayDeque
import java.util.Queue
import kotlin.coroutines.CoroutineContext

/**
 * A [CoroutineDispatcher] that queues work while the [Lifecycle] is not at least [STARTED].
 */
internal class LifecycleCoroutineDispatcher private constructor(
    private val delegate: CoroutineDispatcher,
    private var isStarted: Boolean
) : CoroutineDispatcher(), DefaultLifecycleObserver {

    companion object {
        /**
         * Create a [LifecycleCoroutineDispatcher] that wraps the [delegate].
         */
        @MainThread
        fun create(
            delegate: CoroutineDispatcher,
            lifecycle: Lifecycle
        ): LifecycleCoroutineDispatcher {
            val isStarted = lifecycle.currentState.isAtLeast(STARTED)
            return LifecycleCoroutineDispatcher(delegate, isStarted).apply(lifecycle::addObserver)
        }

        /**
         * Returns [delegate] if [lifecycle] is at least [STARTED].
         * Else, returns a [LifecycleCoroutineDispatcher] that wraps the [delegate].
         */
        @MainThread
        fun createUnlessStarted(
            delegate: CoroutineDispatcher,
            lifecycle: Lifecycle
        ): CoroutineDispatcher {
            val isStarted = lifecycle.currentState.isAtLeast(STARTED)
            return if (isStarted) {
                delegate
            } else {
                LifecycleCoroutineDispatcher(delegate, isStarted).apply(lifecycle::addObserver)
            }
        }
    }

    private val queue: Queue<Pair<CoroutineContext, Runnable>> = ArrayDeque()

    override fun isDispatchNeeded(context: CoroutineContext) = delegate.isDispatchNeeded(context)

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (isStarted) {
            delegate.dispatch(context, block)
        } else {
            queue.offer(context to block)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        isStarted = true
        drainQueue()
    }

    override fun onStop(owner: LifecycleOwner) {
        isStarted = false
    }

    private fun drainQueue() {
        if (queue.isNotEmpty()) {
            val iterator = queue.iterator()
            while (iterator.hasNext()) {
                val (context, block) = iterator.next()
                iterator.remove()
                delegate.dispatch(context, block)
            }
        }
    }
}
