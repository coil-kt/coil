package coil3.network

import coil3.fetch.FetchResult
import kotlin.jvm.JvmField
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
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

class DeDupeInFlightRequestStrategy() : InFlightRequestStrategy {
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
