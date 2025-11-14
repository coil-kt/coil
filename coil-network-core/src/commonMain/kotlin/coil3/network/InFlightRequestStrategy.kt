package coil3.network

import coil3.annotation.ExperimentalCoilApi
import coil3.fetch.FetchResult
import coil3.network.InFlightRequestStrategy.Companion.DEFAULT
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmField
import kotlin.time.Duration
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Interface for in-flight request de-duplication strategies.
 *
 * Implementations coordinate concurrent requests for the same resource key, allowing only one
 * coroutine to perform the actual work (`block()`) while others wait for its result.
 * This helps prevent redundant work (e.g., multiple network requests for the same image)
 * and enables efficient cache utilization.
 *
 * ### Sample: Simple In-Flight Deduplication Strategy
 *
 * The example below ensures that only one coroutine executes the block for a given key.
 * All others wait for the first to finish, and then proceed:
 *
 * ```
 * class SimpleInFlightRequestStrategy : InFlightRequestStrategy {
 *     private val inFlightRequests = mutableMapOf<String, CompletableDeferred<Unit>>()
 *     private val lock = Any()
 *
 *     override suspend fun apply(key: String, block: suspend () -> FetchResult): FetchResult {
 *         var shouldWait = false
 *         val deferred = synchronized(lock) {
 *             inFlightRequests[key]?.also { shouldWait = true }
 *                 ?: CompletableDeferred<Unit>().also { inFlightRequests[key] = it }
 *         }
 *
 *         if (shouldWait) deferred.await()
 *
 *         try {
 *             return block()
 *         } finally {
 *             synchronized(lock) { inFlightRequests.remove(key)?.complete(Unit) }
 *         }
 *     }
 * }
 * ```
 *
 * The [DEFAULT] implementation simply executes the request immediately with **no coordination or
 * de-duplication**.
 * That means every call to `apply` will run `block()` without any concurrency control or waiting.
 */
@ExperimentalCoilApi
interface InFlightRequestStrategy {
    suspend fun apply(key: String, block: (suspend () -> FetchResult)): FetchResult

    companion object {
        /**
         * The default implementation runs the block immediately with no concurrency control.
         * Use a real implementation to avoid duplicated requests.
         */
        @JvmField
        val DEFAULT: InFlightRequestStrategy = DefaultInFlightRequestStrategy()
    }
}

internal class DefaultInFlightRequestStrategy : InFlightRequestStrategy {
    override suspend fun apply(
        key: String,
        block: suspend () -> FetchResult,
    ): FetchResult = block()
}

/**
 * This Class implements an in-flight request de-duplication strategy.
 * It ensures that for a given key, the suspendable `block` (e.g., a network call)
 * is executed primarily by one coroutine at a time.
 *
 * Mechanism:
 * 1. Request Tracking: Keeps an `InFlightRequest` object for each active key.
 * Each `InFlightRequest` contains a `Channel<Unit>` which acts as a signal/communication primitive.
 *
 * 2. Controller Election & State:
 * - Three flags manage the state for the current coroutine's execution:
 * `shouldWaitForSignal`, `currentChannelController`, and `isResponsibleForCleanup`.
 * - The first coroutine to request a key becomes the `currentChannelController`
 * and is also marked `isResponsibleForCleanup`. It creates and stores the `InFlightRequest`.
 * - Subsequent coroutines for the same key are marked `shouldWaitForSignal = true`
 * and suspend by attempting to receive from the request's channel.
 *
 * 3. Baton Passing on Failure:
 * - If a waiting coroutine successfully receives a `Unit` from the channel, it means
 * the previous `currentChannelController` failed and passed the "baton." This
 * waiter then becomes the new `currentChannelController` and `isResponsibleForCleanup`.
 * - If a waiter receives `null` (channel closed), it means the operation likely
 * succeeded or failed terminally. The waiter proceeds to execute `block()`
 * (e.g., to hit a cache) but does not become a controller or responsible for cleanup.
 *
 * 4. Block Execution & Error Handling:
 * - All coroutines eventually execute the provided `block()`.
 * - If `block()` throws an exception for any coroutine:
 * That coroutine will attempt to send a `Unit` signal on the channel
 * (`request.channel.trySend(Unit)`).
 * If this `trySend` is successful (meaning another waiter received the signal and can retry),
 * and if the currently failing coroutine *was* `isResponsibleForCleanup`, it relinquishes
 * this responsibility by setting `isResponsibleForCleanup = false`.
 *
 * 5. Cleanup:
 * - In the `finally` block, if a coroutine is both the `currentChannelController` AND
 * `isResponsibleForCleanup`, it will close the channel and remove the
 * `InFlightRequest` from the map. This ensures resources are freed when the
 * operation succeeds, or when it fails and no other waiter takes over.
 */
class DeDupeInFlightRequestStrategy : InFlightRequestStrategy {
    private val inFlightRequests = mutableMapOf<String, Channel<Unit>>()
    private val lock = SynchronizedObject()

    @OptIn(ExperimentalAtomicApi::class)
    override suspend fun apply(
        key: String,
        block: (suspend () -> FetchResult),
    ): FetchResult {
        var shouldWaitForSignal = true
        var currentChannelController = false
        var isResponsibleForCleanup = false

        val channel = synchronized(lock) {
            inFlightRequests.getOrPut(key) {
                shouldWaitForSignal = false
                currentChannelController = true
                isResponsibleForCleanup = true
                Channel()
            }
        }

        if (shouldWaitForSignal) {
            if (channel.receiveCatching().getOrNull() != null) {
                currentChannelController = true
                isResponsibleForCleanup = true
            }
        }

        try {
            return block()
        } catch (ex: Exception) {
            val successfullyPassedBaton = channel.trySend(Unit).isSuccess
            if (successfullyPassedBaton) {
                isResponsibleForCleanup = false
            }
            throw ex
        } finally {
            if (currentChannelController && isResponsibleForCleanup) {
                synchronized(lock) {
                    channel.close()
                    inFlightRequests.remove(key)
                }
            }
        }
    }
}

/**
 * A de-duplicating in-flight request strategy that allows only one coroutine to execute the
 * critical section (`block`) for a given key at a time, deduplicating concurrent requests and
 * avoiding redundant work (such as repeated network calls).
 *
 * - The first coroutine for a given key becomes the "controller" and executes `block`.
 * - Additional concurrent requests for the same key wait for a signal on a channel.
 * - If the controller succeeds, all waiting requests are unblocked and can continue
 *   (e.g., to use the newly populated cache).
 * - If the controller fails, only one waiting coroutine is unblocked and given a chance to retry
 *   (`block`). This process repeats (baton-passing) until a call succeeds or there are no more
 *   waiters.
 * - Resources and map entries for the key are cleaned up either immediately after completion,
 *   or after a configurable [delay] (to avoid rapid thrashing if new requests for the key arrive
 *   soon after completion).
 *
 * This approach prevents the "thundering herd" problem, ensures only one attempt is made at a
 * time for a resource, and optionally delays cleanup to optimize for bursty request patterns.
 *
 * @property delay Optional delay before removing the request entry from the map after completion.
 *                  This can help prevent repeated creation and removal of in-flight entries if
 *                  requests arrive in quick succession.
 */
class DeDupe2InFlightRequestStrategy(
    val delay: Duration = Duration.ZERO
) : InFlightRequestStrategy {
    private val inFlightRequests = mutableMapOf<String, Request>()
    private val lock = SynchronizedObject()

    override suspend fun apply(
        key: String,
        block: (suspend () -> FetchResult),
    ): FetchResult {
        var shouldWaitForSignal = true

        val state = synchronized(lock) {
            inFlightRequests.getOrPut(key) {
                shouldWaitForSignal = false
                Request(delay)
            }
        }.acquire()

        if (shouldWaitForSignal) {
            state.channel.receiveCatching()
        }

        try {
            return block().also {
                state.succeed()
            }
        } catch (ex: Exception) {
            state.channel.trySend(Unit)
            throw ex
        } finally {
            state.release {
                synchronized(lock) {
                    inFlightRequests.remove(key)
                }
            }
        }
    }

    private class Request(val delay: Duration) : CoroutineScope {
        override val coroutineContext: CoroutineContext
            get() = Dispatchers.Default + Job()
        val channel = Channel<Unit>(Channel.UNLIMITED)

        // Internal state management.
        private val lock = SynchronizedObject()
        private var hasSucceeded = false
        private var isClosed = false
        private var refCount = 0
        private var cancelJob: Job? = null

        fun acquire(): Request = synchronized(lock) {
            refCount++
            this
        }

        fun succeed() = synchronized(lock) {
            hasSucceeded = true
        }

        fun release(cleanup: () -> Unit) = synchronized(lock) {
            if ((--refCount <= 0 || hasSucceeded) && !isClosed) {
                channel.close()
                cancelJob?.cancel()
                if (delay != Duration.ZERO) {
                    cancelJob = launch {
                        delay(delay)
                        cleanup()
                    }
                } else {
                    cleanup()
                }
                isClosed = true
            }
        }
    }
}
