package coil.disk

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class ObservableTestDispatcher(
    private val delegate: TestDispatcher = UnconfinedTestDispatcher()
) : CoroutineDispatcher() {

    private val tasks = ArrayDeque<Pair<CoroutineContext, Runnable>>()
    private val inProgressTasks = AtomicInteger()

    fun isIdle() = sync { tasks.isEmpty() } && inProgressTasks.get() == 0

    fun runNextTask() {
        inProgressTasks.getAndIncrement()
        val (context, block) = sync { tasks.removeFirst() }
        delegate.dispatch(context) {
            try {
                block.run()
            } finally {
                inProgressTasks.getAndDecrement()
            }
        }
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        sync { tasks += context to block }
    }

    private inline fun <T> sync(block: () -> T): T = synchronized(this) { block() }
}
