package coil3.request

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import coil3.util.getLifecycle

/**
 * A [Lifecycle] implementation that is always resumed and never destroyed.
 *
 * This is used as a fallback if [getLifecycle] cannot find a more tightly scoped [Lifecycle].
 */
internal data object GlobalLifecycle : Lifecycle() {

    private val owner = object : LifecycleOwner {
        override val lifecycle get() = this@GlobalLifecycle
    }

    override val currentState get() = State.RESUMED

    override fun addObserver(observer: LifecycleObserver) {
        require(observer is DefaultLifecycleObserver) {
            "$observer must implement androidx.lifecycle.DefaultLifecycleObserver."
        }

        // Call the lifecycle methods in order and do not hold a reference to the observer.
        observer.onCreate(owner)
        observer.onStart(owner)
        observer.onResume(owner)
    }

    override fun removeObserver(observer: LifecycleObserver) {}
}
