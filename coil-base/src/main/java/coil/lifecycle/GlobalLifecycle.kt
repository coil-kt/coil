package coil.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import coil.memory.RequestService

/**
 * A [Lifecycle] implementation that is always resumed and never destroyed.
 *
 * This is used as a fallback if [RequestService.lifecycleInfo] cannot find a more tightly scoped [Lifecycle].
 */
internal object GlobalLifecycle : Lifecycle() {

    override fun addObserver(observer: LifecycleObserver) {}

    override fun removeObserver(observer: LifecycleObserver) {}

    override fun getCurrentState() = State.RESUMED
}
