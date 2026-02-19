package coil3.network

import coil3.annotation.ExperimentalCoilApi
import coil3.fetch.FetchResult
import coil3.network.InFlightRequestStrategy.Companion.DEFAULT
import kotlin.jvm.JvmField
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.channels.Channel

/**
 * Coordinates concurrent requests for the same key.
 *
 * Implementations can reduce duplicate work by running [block] once and making
 * other callers wait for that result.
 */
@ExperimentalCoilApi
interface InFlightRequestStrategy {
    suspend fun apply(key: String, block: suspend () -> FetchResult): FetchResult

    companion object {
        /** Runs [block] immediately with no request coordination. */
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
 * De-duplicates concurrent requests for the same key.
 *
 * The first caller executes [block]. If it succeeds, all waiters are released
 * so they can continue (for example, by reading from cache). If it fails or is
 * cancelled, one waiter is resumed to retry [block].
 */
@ExperimentalCoilApi
class DeDupeInFlightRequestStrategy : InFlightRequestStrategy {
    private val inFlightRequests = mutableMapOf<String, Request>()
    private val lock = SynchronizedObject()

    override suspend fun apply(
        key: String,
        block: suspend () -> FetchResult,
    ): FetchResult {
        var shouldWait = true
        val request = synchronized(lock) {
            inFlightRequests.getOrPut(key) {
                shouldWait = false
                Request()
            }
        }.acquire()

        if (shouldWait) {
            request.channel.receiveCatching()
        }

        try {
            return block().also {
                request.markSucceeded()
            }
        } catch (e: Exception) {
            request.channel.trySend(Unit)
            throw e
        } finally {
            request.release {
                synchronized(lock) {
                    inFlightRequests.remove(key)
                }
            }
        }
    }

    private class Request {
        val channel = Channel<Unit>(Channel.UNLIMITED)

        private val lock = SynchronizedObject()
        private var hasSucceeded = false
        private var isClosed = false
        private var observerCount = 0

        fun acquire(): Request = synchronized(lock) {
            observerCount++
            this
        }

        fun markSucceeded() = synchronized(lock) {
            hasSucceeded = true
        }

        fun release(cleanup: () -> Unit) = synchronized(lock) {
            if ((--observerCount <= 0 || hasSucceeded) && !isClosed) {
                channel.close()
                cleanup()
                isClosed = true
            }
        }
    }
}
