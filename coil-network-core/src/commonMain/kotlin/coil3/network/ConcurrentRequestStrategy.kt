package coil3.network

import coil3.annotation.ExperimentalCoilApi
import coil3.fetch.FetchResult
import kotlin.jvm.JvmField
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.channels.Channel

/**
 * Coordinates concurrent requests for the same key.
 *
 * Implementations can reduce duplicate work by running `block` once and making
 * other callers wait for that result.
 */
@ExperimentalCoilApi
interface ConcurrentRequestStrategy {
    suspend fun apply(key: String, block: suspend () -> FetchResult): FetchResult

    companion object {
        /** Runs `block` immediately with no request coordination. */
        @JvmField val UNCOORDINATED: ConcurrentRequestStrategy = UncoordinatedConcurrentRequestStrategy()
    }
}

private class UncoordinatedConcurrentRequestStrategy : ConcurrentRequestStrategy {
    override suspend fun apply(
        key: String,
        block: suspend () -> FetchResult,
    ): FetchResult = block()
}

/**
 * De-duplicates concurrent requests for the same key.
 *
 * The first caller executes `block`. If it succeeds, all waiters are released
 * so they can continue (for example, by reading from cache). If it fails or is
 * canceled, one waiter is resumed to retry `block`.
 */
@ExperimentalCoilApi
class DeDupeConcurrentRequestStrategy : ConcurrentRequestStrategy {
    private val concurrentRequests = mutableMapOf<String, Request>()
    private val lock = SynchronizedObject()

    override suspend fun apply(
        key: String,
        block: suspend () -> FetchResult,
    ): FetchResult {
        var shouldWait = true
        val request = synchronized(lock) {
            concurrentRequests.getOrPut(key) {
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
                    concurrentRequests -= key
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
