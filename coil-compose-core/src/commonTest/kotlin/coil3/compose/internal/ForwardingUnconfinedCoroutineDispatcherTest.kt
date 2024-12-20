package coil3.compose.internal

import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

class ForwardingUnconfinedCoroutineDispatcherTest {
    private val scheduler = TestCoroutineScheduler()
    private val testDispatcher = TestCoroutineDispatcher()
    private val forwardingDispatcher = ForwardingUnconfinedCoroutineDispatcher(testDispatcher)

    @Test
    fun `does not dispatch when suspended by default`() = runTestWithForwardingDispatcher {
        delay(100.milliseconds)
        assertEquals(0, testDispatcher.dispatchCount)
    }

    @Test
    fun `does not dispatch when unconfined=true`() = runTestWithForwardingDispatcher {
        forwardingDispatcher.unconfined = true
        withContext(Dispatchers.Default) {}
        assertEquals(0, testDispatcher.dispatchCount)
    }

    @Test
    fun `does dispatch when unconfined=false`() = runTestWithForwardingDispatcher {
        forwardingDispatcher.unconfined = false
        withContext(Dispatchers.Default) {}
        assertEquals(1, testDispatcher.dispatchCount)
    }

    private fun runTestWithForwardingDispatcher(
        testBody: suspend CoroutineScope.() -> Unit,
    ) = runTest(forwardingDispatcher, testBody = testBody)

    private class TestCoroutineDispatcher : CoroutineDispatcher() {
        var dispatchCount = 0
            private set

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            dispatchCount++
            block.run()
        }
    }
}
