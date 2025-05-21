package coil3.network

import coil3.fetch.FetchResult
import kotlin.jvm.JvmField
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val mutex: Mutex = Mutex()

    override suspend fun apply(
        key: String,
        block: (suspend () -> FetchResult),
    ): FetchResult {
        var shouldWait = false
        val request = mutex.withLock {
            inFlightRequests[key]?.also { shouldWait = true } ?: addRequest(key)
        }

        if (shouldWait) {
            request.deferrable.await()
        }

        return request.use {
            block()
        }.also {
            mutex.withLock {
                inFlightRequests.remove(key)
            }
        }
    }

    // must be called with the Mutex already acquired
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
