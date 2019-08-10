package coil.lifecycle

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext

class FakeCoroutineDispatcher : CoroutineDispatcher() {

    val dispatches = mutableListOf<Pair<CoroutineContext, Runnable>>()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatches += context to block
    }
}
