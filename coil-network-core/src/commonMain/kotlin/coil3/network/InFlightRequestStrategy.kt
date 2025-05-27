package coil3.network

import coil3.fetch.FetchResult
import kotlin.jvm.JvmField
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import okio.Closeable
import okio.use

interface InFlightRequestStrategy {
    suspend fun apply(key: String, block: (suspend () -> FetchResult)): FetchResult {
        return block()
    }

    companion object {
        @JvmField
        val DEFAULT: InFlightRequestStrategy = object : InFlightRequestStrategy {}
    }
}

/**
 * Implements a basic in-flight request de-duplication strategy.
 * If multiple requests for the same key arrive concurrently, the `block`
 * lambda is executed once by the first request.
 * Subsequent requests wait for the first one to complete before proceeding.
 *
 * Mechanism:
 * 1. First Request: The first coroutine for a key creates the `InFlightRequest`,
 * stores it, and proceeds to execute the `block`.
 * 2. Subsequent Requests: If an `InFlightRequest` for the key already exists,
 * subsequent coroutines suspend by calling `await()` on the `CompletableDeferred`
 * held by that existing request.
 * 3. Unblocking Waiters: The `block` is executed within a `request.use { ... }`
 * construct. The `InFlightRequest.close()` method, which is called when the
 * `use` block finishes (whether `block()` succeeds or throws an exception),
 * completes the `CompletableDeferred`. This resumes all waiting coroutines.
 * 4. Execution by Waiters: After being unblocked, the waiting coroutines will then
 * also proceed to execute the `block()` themselves (at which point, if the first
 * call populated a cache, they should hit the cache).
 * 5. Cleanup: Finally, the `InFlightRequest` entry for the key is removed from the
 * map.
 *
 * Note: This strategy unblocks all waiters regardless of whether the initial `block()`
 * execution succeeded or failed, as long as the deferred is completed.
 */
class SimpleInFlightRequestStrategy() : InFlightRequestStrategy {
    private val inFlightRequests = mutableMapOf<String, InFlightRequest>()
    private val lock = SynchronizedObject()

    override suspend fun apply(
        key: String,
        block: (suspend () -> FetchResult),
    ): FetchResult {
        var shouldWait = true
        val request = synchronized(lock) {
            inFlightRequests.getOrPut(key) {
                shouldWait = false
                InFlightRequest(CompletableDeferred())
            }
        }

        if (shouldWait) {
            request.deferrable.await()
        }

        try {
            return request.use {
                block()
            }
        } finally {
            synchronized(lock) {
                inFlightRequests.remove(key)
            }
        }
    }

    data class InFlightRequest(
        val deferrable: CompletableDeferred<Unit>,
    ) : Closeable {
        override fun close() {
            deferrable.complete(Unit)
        }
    }
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
class DeDupeInFlightRequestStrategy() : InFlightRequestStrategy {
    private val inFlightRequests = mutableMapOf<String, InFlightRequest>()
    private val lock = SynchronizedObject()

    override suspend fun apply(
        key: String,
        block: (suspend () -> FetchResult),
    ): FetchResult {
        var shouldWaitForSignal = true
        var currentChannelController = false
        var isResponsibleForCleanup = false

        val request = synchronized(lock) {
            inFlightRequests.getOrPut(key) {
                shouldWaitForSignal = false
                currentChannelController = true
                isResponsibleForCleanup = true
                InFlightRequest(key)
            }
        }

        if (shouldWaitForSignal) {
            if (request.channel.receiveCatching().getOrNull() != null) {
                currentChannelController = true
                isResponsibleForCleanup = true
            }
        }

        try {
            return block()
        } catch (ex: Exception) {
            val successfullyPassedBaton = request.channel.trySend(Unit).isSuccess
            if (successfullyPassedBaton) {
                isResponsibleForCleanup = false
            }
            throw ex
        } finally {
            if (currentChannelController && isResponsibleForCleanup) {
                synchronized(lock) {
                    request.channel.close()
                    inFlightRequests.remove(key)
                }
            }
        }
    }

    data class InFlightRequest(
        val key: String,
        val channel: Channel<Unit> = Channel()
    )
}
