package coil.lifecycle

import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.Runnable
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class LifecycleCoroutineDispatcherTest {

    private lateinit var dispatcher: FakeCoroutineDispatcher
    private lateinit var lifecycle: FakeLifecycleRegistry

    private lateinit var context: CoroutineContext
    private lateinit var runnable: Runnable

    @Before
    fun before() {
        dispatcher = FakeCoroutineDispatcher()
        lifecycle = FakeLifecycleRegistry()

        context = EmptyCoroutineContext
        runnable = Runnable { }
    }

    @Test
    fun `dispatches if already started`() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)

        val lifecycleDispatcher = LifecycleCoroutineDispatcher.create(dispatcher, lifecycle)
        lifecycleDispatcher.dispatch(context, runnable)

        assertEquals(listOf(context to runnable), dispatcher.dispatches)
    }

    @Test
    fun `dispatches after start event`() {
        val lifecycleDispatcher = LifecycleCoroutineDispatcher.create(dispatcher, lifecycle)
        lifecycleDispatcher.dispatch(context, runnable)

        assertEquals(emptyList<Pair<CoroutineContext, Runnable>>(), dispatcher.dispatches)

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)

        assertEquals(listOf(context to runnable), dispatcher.dispatches)
    }

    @Test
    fun `queues when stopped`() {
        val lifecycleDispatcher = LifecycleCoroutineDispatcher.create(dispatcher, lifecycle)
        lifecycleDispatcher.dispatch(context, runnable)

        assertEquals(emptyList<Pair<CoroutineContext, Runnable>>(), dispatcher.dispatches)

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)

        assertEquals(listOf(context to runnable), dispatcher.dispatches)

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)

        lifecycleDispatcher.dispatch(context, runnable)

        assertEquals(listOf(context to runnable), dispatcher.dispatches)
    }
}
