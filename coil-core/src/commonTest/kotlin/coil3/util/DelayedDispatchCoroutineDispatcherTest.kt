package coil3.util

import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

class DelayedDispatchCoroutineDispatcherTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val delayedDispatcher = DelayedDispatchCoroutineDispatcher(testDispatcher)

    @Test
    fun `does not dispatch when suspended by default`() = runTestWithDelayedDispatch {
        delay(100.milliseconds)
        assertFalse { testDispatcher.dispatched }
    }

    @Test
    fun `does not dispatch when dispatchEnabled=false`() = runTestWithDelayedDispatch {
        delayedDispatcher.dispatchEnabled = false
        withContext(Dispatchers.Default) {}
        assertFalse { testDispatcher.dispatched }
    }

    @Test
    fun `does dispatch when dispatchEnabled=true`() = runTestWithDelayedDispatch {
        delayedDispatcher.dispatchEnabled = true
        withContext(Dispatchers.Default) {}
        assertTrue { testDispatcher.dispatched }
    }

    private fun runTestWithDelayedDispatch(
        testBody: suspend CoroutineScope.() -> Unit,
    ) = runTest { withContext(delayedDispatcher, testBody) }

    private class TestCoroutineDispatcher : CoroutineDispatcher() {
        var dispatched = false
            private set

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            dispatched = true
            block.run()
        }
    }
}
