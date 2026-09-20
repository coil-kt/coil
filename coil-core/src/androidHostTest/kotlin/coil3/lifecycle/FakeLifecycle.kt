package coil3.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver

class FakeLifecycle : Lifecycle() {

    val observers = mutableListOf<LifecycleObserver>()

    override var currentState: State = State.INITIALIZED

    override fun addObserver(observer: LifecycleObserver) {
        observers += observer
    }

    override fun removeObserver(observer: LifecycleObserver) {
        observers -= observer
    }
}
