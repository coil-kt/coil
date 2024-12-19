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

class ForwardingUnconfinedCoroutineDispatcherTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val forwardingDispatcher = ForwardingUnconfinedCoroutineDispatcher(testDispatcher)

    @Test
    fun `does not dispatch when suspended by default`() = test {
        delay(100.milliseconds)
        assertFalse { testDispatcher.dispatched }
    }

    @Test
    fun `does not dispatch when unconfined=true`() = test {
        forwardingDispatcher.unconfined = true
        withContext(Dispatchers.Default) {}
        assertFalse { testDispatcher.dispatched }
    }

    @Test
    fun `does dispatch when unconfined=false`() = test {
        forwardingDispatcher.unconfined = false
        withContext(Dispatchers.Default) {}
        assertTrue { testDispatcher.dispatched }
    }

    private fun test(
        testBody: suspend CoroutineScope.() -> Unit,
    ) = runTest { withContext(forwardingDispatcher, testBody) }

    private class TestCoroutineDispatcher : CoroutineDispatcher() {
        var dispatched = false
            private set

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            dispatched = true
            block.run()
        }
    }
}
