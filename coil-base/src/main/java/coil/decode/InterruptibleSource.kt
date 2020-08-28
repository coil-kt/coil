package coil.decode

import coil.annotation.InternalCoilApi
import coil.util.loop
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Buffer
import okio.ForwardingSource
import okio.Source
import java.io.InterruptedIOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/**
 * Wraps [source] to support thread interruption while inside [block].
 *
 * Consumers **should not** read [source] inside [block]. Instead, read from the [Source] provided to [block].
 */
@InternalCoilApi
suspend inline fun <T> withInterruptibleSource(
    source: Source,
    crossinline block: (Source) -> T
): T = suspendCancellableCoroutine { continuation ->
    try {
        val interruptibleSource = InterruptibleSource(continuation, source)
        try {
            continuation.resume(block(interruptibleSource))
        } finally {
            interruptibleSource.clearInterrupt()
        }
    } catch (exception: Exception) {
        // Convert InterruptedExceptions -> CancellationExceptions so the job is treated as cancelled.
        if (exception is InterruptedException || exception is InterruptedIOException) {
            throw CancellationException("Blocking call was interrupted due to parent cancellation.").initCause(exception)
        } else {
            throw exception
        }
    }
}

private const val WORKING = 0
private const val UNINTERRUPTIBLE = 1
private const val FINISHED = 2
private const val PENDING = 3
private const val INTERRUPTING = 4
private const val INTERRUPTED = 5

/** A [ForwardingSource] that prevents interrupting the current thread while reading from [delegate]. */
@PublishedApi
internal class InterruptibleSource(
    continuation: CancellableContinuation<*>,
    delegate: Source
) : ForwardingSource(delegate), CompletionHandler {

    private val _state = AtomicInteger(UNINTERRUPTIBLE)
    private val targetThread = Thread.currentThread()

    init {
        continuation.invokeOnCancellation(this)

        // Ensure that we start in a valid state.
        run {
            _state.loop { state ->
                when (state) {
                    UNINTERRUPTIBLE -> if (_state.compareAndSet(state, UNINTERRUPTIBLE)) return@run
                    PENDING, INTERRUPTING, INTERRUPTED -> return@run
                    else -> invalidState(state)
                }
            }
        }
    }

    override fun read(sink: Buffer, byteCount: Long): Long {
        try {
            setInterruptible(false)
            return super.read(sink, byteCount)
        } finally {
            setInterruptible(true)
        }
    }

    /** Enable/disable interruption. */
    private fun setInterruptible(interruptible: Boolean) {
        _state.loop { state ->
            when (state) {
                // Working/uninterruptible: update the current state (even if it is the same).
                WORKING, UNINTERRUPTIBLE -> {
                    val newState = if (interruptible) WORKING else UNINTERRUPTIBLE
                    if (_state.compareAndSet(state, newState)) return
                }
                // Pending: we attempted to interrupt earlier. Interrupt now.
                PENDING -> if (_state.compareAndSet(state, INTERRUPTING)) {
                    targetThread.interrupt()
                    _state.set(INTERRUPTED)
                    return
                }
                INTERRUPTING -> {
                    // Spin. The CompletionHandler is interrupting our thread right now and
                    // we have to wait for it and then clear the interrupt status.
                }
                INTERRUPTED -> {
                    // Clear the thread's interrupted status and return.
                    Thread.interrupted()
                    return
                }
                else -> invalidState(state)
            }
        }
    }

    /** Prevent the thread's interrupted state from leaking. */
    fun clearInterrupt() {
        _state.loop { state ->
            when (state) {
                WORKING, PENDING -> if (_state.compareAndSet(state, FINISHED)) return
                INTERRUPTING -> {
                    // Spin. The CompletionHandler is interrupting our thread right now and
                    // we have to wait for it and then clear the interrupt status.
                }
                INTERRUPTED -> {
                    // Clear the thread's interrupted status and return.
                    Thread.interrupted()
                    return
                }
                else -> invalidState(state)
            }
        }
    }

    /** @see CompletionHandler */
    override fun invoke(cause: Throwable?) {
        _state.loop { state ->
            when (state) {
                // Working: attempt to interrupt the thread.
                WORKING -> if (_state.compareAndSet(state, INTERRUPTING)) {
                    targetThread.interrupt()
                    _state.set(INTERRUPTED)
                    return
                }
                // Uninterruptible: update the state to mark the interrupt as pending.
                UNINTERRUPTIBLE -> if (_state.compareAndSet(state, PENDING)) return
                // Ignore other states.
                FINISHED, PENDING, INTERRUPTING, INTERRUPTED -> return
                else -> invalidState(state)
            }
        }
    }

    private fun invalidState(state: Int): Nothing = error("Illegal state: $state")
}
