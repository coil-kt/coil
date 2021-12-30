package coil.disk

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

/**
 * A simple test dispatcher that queues all tasks and executes one when [runNextTask] is called.
 */
class SimpleTestDispatcher : CoroutineDispatcher() {

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
