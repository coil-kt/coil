package coil3.network

import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.ImageFetchResult
import coil3.test.utils.FakeImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield

class ConcurrentRequestStrategyTest {

    @Test
    fun uncoordinated_same_key_requests_run_in_parallel() = runTest {
        val strategy = ConcurrentRequestStrategy.UNCOORDINATED
        val started = Channel<Int>(capacity = 2)
        val finishRequests = CompletableDeferred<Unit>()

        val first = async {
            strategy.apply("key") {
                started.send(1)
                finishRequests.await()
                newFetchResult(1)
            }
        }
        val second = async {
            strategy.apply("key") {
                started.send(2)
                finishRequests.await()
                newFetchResult(2)
            }
        }

        assertEquals(3, started.receive() + started.receive())
        finishRequests.complete(Unit)

        assertIs<ImageFetchResult>(first.await())
        assertIs<ImageFetchResult>(second.await())
    }

    @Test
    fun dedupe_same_key_waits_for_in_flight_request() = runTest {
        val strategy = DeDupeConcurrentRequestStrategy()
        val firstStarted = CompletableDeferred<Unit>()
        val finishFirst = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()

        val first = async {
            strategy.apply("key") {
                firstStarted.complete(Unit)
                finishFirst.await()
                newFetchResult(1)
            }
        }
        firstStarted.await()

        val second = async {
            strategy.apply("key") {
                secondStarted.complete(Unit)
                newFetchResult(2)
            }
        }

        yield()
        assertFalse(secondStarted.isCompleted)

        finishFirst.complete(Unit)
        assertIs<ImageFetchResult>(first.await())

        yield()
        assertTrue(secondStarted.isCompleted)
        assertIs<ImageFetchResult>(second.await())
    }

    @Test
    fun dedupe_failed_request_resumes_single_waiter() = runTest {
        val strategy = DeDupeConcurrentRequestStrategy()
        val started = Channel<Int>(capacity = Channel.UNLIMITED)
        val attemptsLock = Mutex()
        var attempts = 0
        val failLeader = CompletableDeferred<Unit>()
        val allowRetryToSucceed = CompletableDeferred<Unit>()

        suspend fun fetch(): FetchResult {
            val attempt = attemptsLock.withLock {
                attempts += 1
                attempts
            }
            started.send(attempt)
            return when (attempt) {
                1 -> {
                    failLeader.await()
                    error("leader failed")
                }
                2 -> {
                    allowRetryToSucceed.await()
                    newFetchResult(attempt)
                }
                else -> newFetchResult(attempt)
            }
        }

        val leader = async { runCatching { strategy.apply("key", ::fetch) } }
        val waiter1 = async { runCatching { strategy.apply("key", ::fetch) } }
        val waiter2 = async { runCatching { strategy.apply("key", ::fetch) } }

        assertEquals(1, started.receive())
        failLeader.complete(Unit)
        assertTrue(leader.await().isFailure)

        assertEquals(2, started.receive())
        yield()
        assertEquals(2, attemptsLock.withLock { attempts })

        allowRetryToSucceed.complete(Unit)
        assertTrue(waiter1.await().isSuccess)
        assertTrue(waiter2.await().isSuccess)
        assertEquals(3, started.receive())
        assertEquals(3, attemptsLock.withLock { attempts })
    }

    @Test
    fun dedupe_canceled_request_resumes_waiter() = runTest {
        val strategy = DeDupeConcurrentRequestStrategy()
        val started = Channel<Int>(capacity = 2)
        val cancelLeader = CompletableDeferred<Unit>()
        val waiterStarted = CompletableDeferred<Unit>()

        val leader = async {
            runCatching {
                strategy.apply("key") {
                    started.send(1)
                    cancelLeader.await()
                    throw CancellationException("leader canceled")
                }
            }
        }
        assertEquals(1, started.receive())

        val waiter = async {
            strategy.apply("key") {
                waiterStarted.complete(Unit)
                started.send(2)
                newFetchResult(2)
            }
        }

        yield()
        assertFalse(waiterStarted.isCompleted)

        cancelLeader.complete(Unit)
        assertTrue(leader.await().exceptionOrNull() is CancellationException)
        assertEquals(2, started.receive())
        assertIs<ImageFetchResult>(waiter.await())
    }

    @Test
    fun dedupe_different_keys_are_independent() = runTest {
        val strategy = DeDupeConcurrentRequestStrategy()
        val started = Channel<String>(capacity = Channel.UNLIMITED)
        val finishFirstA = CompletableDeferred<Unit>()
        val secondAStarted = CompletableDeferred<Unit>()

        val firstA = async {
            strategy.apply("a") {
                started.send("a1")
                finishFirstA.await()
                newFetchResult(1)
            }
        }
        assertEquals("a1", started.receive())

        val secondA = async {
            strategy.apply("a") {
                secondAStarted.complete(Unit)
                started.send("a2")
                newFetchResult(2)
            }
        }
        val firstB = async {
            strategy.apply("b") {
                started.send("b1")
                newFetchResult(3)
            }
        }

        assertEquals("b1", started.receive())
        assertFalse(secondAStarted.isCompleted)

        finishFirstA.complete(Unit)
        assertIs<ImageFetchResult>(firstA.await())

        yield()
        assertTrue(secondAStarted.isCompleted)
        assertEquals("a2", started.receive())
        assertIs<ImageFetchResult>(secondA.await())
        assertIs<ImageFetchResult>(firstB.await())
    }

    @Test
    fun dedupe_failed_request_without_waiters_is_cleaned_up() = runTest {
        val strategy = DeDupeConcurrentRequestStrategy()

        val first = runCatching {
            strategy.apply("key") {
                error("request failed")
            }
        }
        assertTrue(first.isFailure)

        var wasCalled = false
        val second = strategy.apply("key") {
            wasCalled = true
            newFetchResult(1)
        }
        assertTrue(wasCalled)
        assertIs<ImageFetchResult>(second)
    }

    private fun newFetchResult(id: Int): FetchResult {
        return ImageFetchResult(
            image = FakeImage(width = id, height = id),
            isSampled = false,
            dataSource = DataSource.NETWORK,
        )
    }
}
