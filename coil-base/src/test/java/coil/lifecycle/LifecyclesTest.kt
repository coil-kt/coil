package coil.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import coil.util.awaitStarted
import coil.util.createTestMainDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LifecyclesTest {

    private lateinit var mainDispatcher: TestDispatcher
    private lateinit var lifecycle: FakeLifecycleRegistry

    @Before
    fun before() {
        mainDispatcher = createTestMainDispatcher(standard = true)
        lifecycle = FakeLifecycleRegistry()
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun `does not observe if already started`() = runTest {
        val lifecycle = object : Lifecycle() {
            override fun getCurrentState() = State.STARTED
            override fun addObserver(observer: LifecycleObserver) = fail("Should not observe.")
            override fun removeObserver(observer: LifecycleObserver) = fail("Should not observe.")
        }
        lifecycle.awaitStarted()
    }

    @Test
    fun `dispatches after start event`() = runTest {
        assertEquals(0, lifecycle.observerCount)

        val job = launch { lifecycle.awaitStarted() }

        assertFalse(job.isCompleted)
        assertEquals(1, lifecycle.observerCount)

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)

        assertTrue(job.isCompleted)
        assertEquals(0, lifecycle.observerCount)
    }

    @Test
    fun `observer is removed if cancelled`() = runTest {
        assertEquals(0, lifecycle.observerCount)

        val job = launch { lifecycle.awaitStarted() }

        assertFalse(job.isCompleted)
        assertEquals(1, lifecycle.observerCount)

        job.cancel()

        assertTrue(job.isCompleted)
        assertEquals(0, lifecycle.observerCount)
    }
}
