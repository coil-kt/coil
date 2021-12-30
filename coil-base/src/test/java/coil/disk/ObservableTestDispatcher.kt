package coil.disk

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class ObservableTestDispatcher : CoroutineDispatcher() {

    private val tasks = ConcurrentLinkedQueue<Runnable>()
    private val inProgressTasks = AtomicInteger()

    fun isIdle() = tasks.isEmpty() && inProgressTasks.get() == 0

    fun runNextTask() {
        val block = tasks.remove()
        try {
            inProgressTasks.getAndIncrement()
            block.run()
        } finally {
            inProgressTasks.getAndDecrement()
        }
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        tasks += block
    }
}
