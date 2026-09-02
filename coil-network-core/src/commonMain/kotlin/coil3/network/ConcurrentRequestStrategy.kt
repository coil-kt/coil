package coil3.network

import coil3.annotation.ExperimentalCoilApi
import coil3.fetch.FetchResult
import kotlin.jvm.JvmField
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred

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
    private val concurrentRequests = mutableMapOf<String, CompletableDeferred<Boolean>>()
    private val lock = SynchronizedObject()

    override suspend fun apply(
        key: String,
        block: suspend () -> FetchResult,
    ): FetchResult {
        while (true) {
            var isLeader = false
            val request = synchronized(lock) {
                concurrentRequests.getOrPut(key) {
                    isLeader = true
                    CompletableDeferred()
                }
            }

            if (!isLeader) {
                val succeeded = request.await()
                if (succeeded) {
                    return block()
                }
                continue
            }

            var succeeded = false
            try {
                return block().also {
                    succeeded = true
                }
            } finally {
                synchronized(lock) {
                    concurrentRequests -= key
                }
                request.complete(succeeded)
            }
        }
    }
}
