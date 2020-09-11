package coil.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import coil.util.awaitStarted
import coil.util.createTestMainDispatcher
import coil.util.runBlockingTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class LifecyclesTest {

    private lateinit var mainDispatcher: TestCoroutineDispatcher
    private lateinit var lifecycle: FakeLifecycleRegistry

    @Before
    fun before() {
        mainDispatcher = createTestMainDispatcher()
        lifecycle = FakeLifecycleRegistry()
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun `does not observe if already started`() = runBlockingTest {
        val lifecycle = object : Lifecycle() {
            override fun getCurrentState() = State.STARTED
            override fun addObserver(observer: LifecycleObserver) = error("Should not observe.")
            override fun removeObserver(observer: LifecycleObserver) = error("Should not observe.")
        }
        lifecycle.awaitStarted()
    }

    @Test
    fun `dispatches after start event`() = runBlockingTest {
        assertEquals(0, lifecycle.observerCount)

        val job = launch { lifecycle.awaitStarted() }

        assertFalse(job.isCompleted)
        assertEquals(1, lifecycle.observerCount)

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)

        assertTrue(job.isCompleted)
        assertEquals(0, lifecycle.observerCount)
    }

    @Test
    fun `observer is removed if cancelled`() = runBlockingTest {
        assertEquals(0, lifecycle.observerCount)

        val job = launch { lifecycle.awaitStarted() }

        assertFalse(job.isCompleted)
        assertEquals(1, lifecycle.observerCount)

        job.cancel()

        assertTrue(job.isCompleted)
        assertEquals(0, lifecycle.observerCount)
    }
}
