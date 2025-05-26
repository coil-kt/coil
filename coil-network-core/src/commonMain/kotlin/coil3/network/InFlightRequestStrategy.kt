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

class SimpleInFlightRequestStrategy() : InFlightRequestStrategy {
    private val inFlightRequests = mutableMapOf<String, InFlightRequest>()
    private val lock = SynchronizedObject()

    override suspend fun apply(
        key: String,
        block: (suspend () -> FetchResult),
    ): FetchResult {
        var shouldWait = false
        val request = synchronized(lock) {
            inFlightRequests[key]?.also { shouldWait = true } ?: addRequest(key)
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

    // must be called with the lock already acquired
    private fun addRequest(key: String): InFlightRequest {
        return InFlightRequest(key, CompletableDeferred()).also { inFlightRequests[key] = it }
    }

    data class InFlightRequest(
        val key: String,
        val deferrable: CompletableDeferred<Unit>,
    ) : Closeable {
        override fun close() {
            deferrable.complete(Unit)
        }
    }
}

class DeDupeInFlightRequestStrategy() : InFlightRequestStrategy {
    private val inFlightRequests = mutableMapOf<String, InFlightRequest>()
    private val lock = SynchronizedObject()

    override suspend fun apply(
        key: String,
        block: (suspend () -> FetchResult),
    ): FetchResult {
        // This coroutine found an existing request and should attempt to wait on its channel.
        var shouldWaitForSignal = false
        // This coroutine is controlling the channel. This can be the first coroutine for the key
        // or one that received the baton after a prior failure
        var currentChannelController = false
        // Is this channel controller also responsible for cleaning up the channel and map entry.
        // A controller might not be responsible for cleanup if it successfully passes the baton to
        // another waiter upon its own failure.
        var isResponsibleForCleanup = false

        val request = synchronized(lock) {
            inFlightRequests[key]?.also {
                // An InFlightRequest already exists for this key. This coroutine must wait.
                shouldWaitForSignal = true
            } ?: addRequest(key).also {
                // No existing request. This coroutine is the first and the channel controller
                currentChannelController = true
                // Initially responsible for cleanup.
                isResponsibleForCleanup = true
            }
        }

        if (shouldWaitForSignal) {
            // This coroutine is a waiter. It suspends until the channel receives or is closed.
            // Receiving Unit means a previous executor failed, and this waiter should take over.
            if (request.channel.receiveCatching().getOrNull() != null) {
                // Successfully received a signal (Unit). This waiter is now the channel controller.
                currentChannelController = true
                // And also responsible for cleaning up after success.
                isResponsibleForCleanup = true
            } else {
                // Channel was closed (by a successful executor or the last in a chain of failures).
                // This waiter will proceed to run block() hoping for a cache hit.
                // It's not the channel controller for this attempt post-signal, nor
                // responsible for cleanup.
            }
        }

        // If !currentChannelController at this point, it means:
        // 1. This coroutine was a waiter.
        // 2. It didn't receive a direct signal (Unit) to become the next executor
        //    (channel was likely closed).
        // 3. It will run block() (e.g., to hit cache) but won't manage the channel or map entry.
        try {
            return block()
        } catch (ex: Exception) {
            // This coroutine was the designated executor and its block() failed.
            // Attempt to pass execution responsibility (baton) to another waiting coroutine.
            val successfullyPassedBaton = request.channel.trySend(Unit).isSuccess
            if (successfullyPassedBaton) {
                // Baton was passed. This coroutine is no longer the channel controller nor
                // responsible for cleanup.
                currentChannelController = false
                isResponsibleForCleanup = false
            } else {
                // The baton was not passed (no waiter or channel closed), this coroutine remains
                // the channel controller and responsible for the cleanup in the finally block.
            }
            throw ex
        } finally {
            if (currentChannelController && isResponsibleForCleanup) {
                // This coroutine was the channel owner AND is responsible for cleanup.
                // This happens if:
                // 1. It executed block() successfully.
                // 2. It executed block(), failed, AND could not pass the baton to a waiter.
                synchronized(lock) {
                    request.channel.close()
                    inFlightRequests.remove(key)
                }
            }
        }
    }

    // must be called with the lock already acquired
    private fun addRequest(key: String): InFlightRequest {
        return InFlightRequest(key).also { inFlightRequests[key] = it }
    }

    data class InFlightRequest(
        val key: String,
        val channel: Channel<Unit> = Channel()
    )
}
