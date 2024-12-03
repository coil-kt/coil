package coil3.compose.internal

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
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

    @Test
    fun `does not dispatch when suspended`() = runTestWithDelayedDispatch(
        context = DelayedDispatchCoroutineDispatcher(testDispatcher),
    ) {
        delay(100.milliseconds)
        assertFalse { testDispatcher.dispatched }
    }

    @Test
    fun `does not dispatch when CoroutineDispatcher is not changed`() = runTestWithDelayedDispatch(
        context = DelayedDispatchCoroutineDispatcher(testDispatcher),
    ) {
        withContext(TestCoroutineContextMarker()) {}
        assertFalse { testDispatcher.dispatched }
    }

    @Test
    fun `does not dispatch with EmptyCoroutineContext`() = runTestWithDelayedDispatch(
        context = DelayedDispatchCoroutineDispatcher(testDispatcher),
    ) {
        withContext(EmptyCoroutineContext) {}
        assertFalse { testDispatcher.dispatched }
    }

    @Test
    fun `does not dispatch with Dispatchers_Unconfined`() = runTestWithDelayedDispatch(
        context = DelayedDispatchCoroutineDispatcher(testDispatcher),
    ) {
        withContext(Dispatchers.Unconfined) {}
        assertFalse { testDispatcher.dispatched }
    }

    @Test
    fun `does dispatch with Dispatchers_Default`() = runTestWithDelayedDispatch(
        context = DelayedDispatchCoroutineDispatcher(testDispatcher),
    ) {
        withContext(Dispatchers.Default) {}
        assertTrue { testDispatcher.dispatched }
    }

    private fun runTestWithDelayedDispatch(
        context: CoroutineContext = EmptyCoroutineContext,
        testBody: suspend CoroutineScope.() -> Unit,
    ) = runTest { withContext(context, testBody) }

    private class TestCoroutineContextMarker : AbstractCoroutineContextElement(Key) {
        object Key : CoroutineContext.Key<TestCoroutineContextMarker>
    }

    private class TestCoroutineDispatcher : CoroutineDispatcher() {
        var dispatched = false
            private set

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            dispatched = true
            block.run()
        }
    }
}
