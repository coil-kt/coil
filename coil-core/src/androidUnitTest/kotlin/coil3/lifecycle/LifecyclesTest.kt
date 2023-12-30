package coil3.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import coil3.test.utils.RobolectricTest
import coil3.util.awaitStarted
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LifecyclesTest : RobolectricTest() {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var lifecycle: FakeLifecycleRegistry

    @Before
    fun before() {
        testDispatcher = UnconfinedTestDispatcher()
        lifecycle = FakeLifecycleRegistry()
    }

    @Test
    fun `does not observe if already started`() = runTest(testDispatcher) {
        val lifecycle = object : Lifecycle() {
            override val currentState get() = State.STARTED
            override fun addObserver(observer: LifecycleObserver) = fail("Should not observe.")
            override fun removeObserver(observer: LifecycleObserver) = fail("Should not observe.")
        }
        lifecycle.awaitStarted()
    }

    @Test
    fun `dispatches after start event`() = runTest(testDispatcher) {
        assertEquals(0, lifecycle.observerCount)

        val job = launch { lifecycle.awaitStarted() }

        assertFalse(job.isCompleted)
        assertEquals(1, lifecycle.observerCount)

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)

        assertTrue(job.isCompleted)
        assertEquals(0, lifecycle.observerCount)
    }

    @Test
    fun `observer is removed if cancelled`() = runTest(testDispatcher) {
        assertEquals(0, lifecycle.observerCount)

        val job = launch { lifecycle.awaitStarted() }

        assertFalse(job.isCompleted)
        assertEquals(1, lifecycle.observerCount)

        job.cancel()

        assertTrue(job.isCompleted)
        assertEquals(0, lifecycle.observerCount)
    }
}
