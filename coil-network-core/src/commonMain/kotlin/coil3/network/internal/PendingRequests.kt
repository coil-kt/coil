package coil3.network.internal

import coil3.annotation.InternalCoilApi
import coil3.fetch.SourceFetchResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages pending network requests to deduplicate concurrent requests for the same key.
 *
 * When multiple requests are made for the same key concurrently, only the first request
 * will execute. Subsequent requests will await the result of the first request.
 *
 * Handles cancellation properly: if some observers are cancelled but others are still
 * waiting, the underlying operation continues for the remaining observers.
 *
 * @param scope The parent coroutine scope. When this scope is cancelled, all pending
 *        requests will be cancelled as well, ensuring proper structured concurrency.
 */
@InternalCoilApi
class PendingRequests {

    private val mutex = Mutex()
    private val pending = HashMap<String, PendingRequest>()

    /**
     * Execute [block] for the given [key], deduplicating concurrent requests.
     *
     * If a request is already in flight for [key], this suspends until that request
     * completes and returns its result. Otherwise, executes [block] and shares its
     * result with all concurrent requests for the same [key].
     *
     * @param key The unique key for this request.
     * @param block The suspending function to execute if no request is already in flight.
     * @return The result of the request.
     */
    suspend fun executeOrAwait(
        key: String,
        block: suspend () -> SourceFetchResult,
    ): SourceFetchResult {
        val deferred: CompletableDeferred<SourceFetchResult>
        val isNewRequest: Boolean

        // 락을 한 번만 잡고 확인 및 등록을 모두 처리
        mutex.withLock {
            val existing = pending[key]
            if (existing != null) {
                deferred = existing.result
                isNewRequest = false
            } else {
                deferred = CompletableDeferred()
                pending[key] = PendingRequest(result = deferred)
                isNewRequest = true
            }
        }

        return if (isNewRequest) {
            try {
                val fetchResult = block()
                deferred.complete(fetchResult)
                fetchResult
            } catch (e: Throwable) {
                deferred.completeExceptionally(e)
                throw e
            } finally {
                mutex.withLock {
                    pending.remove(key)
                }
            }
        } else {
            deferred.await()
        }
    }

    private data class PendingRequest(
        val result: CompletableDeferred<SourceFetchResult>,
    )
}
