package coil3.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import coil3.annotation.MainThread
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** Suspend until [Lifecycle.currentState] is at least [STARTED] */
@MainThread
internal suspend fun Lifecycle.awaitStarted() {
    // Fast path: we're already started.
    if (currentState.isAtLeast(STARTED)) return

    // Slow path: observe the lifecycle until we're started.
    var observer: LifecycleObserver? = null
    try {
        suspendCancellableCoroutine { continuation ->
            observer = object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    continuation.resume(Unit)
                }
            }
            addObserver(observer!!)
        }
    } finally {
        // 'observer' will always be null if this method is marked as 'inline'.
        observer?.let(::removeObserver)
    }
}

/** Remove and re-add the observer to ensure all its lifecycle callbacks are invoked. */
@MainThread
internal fun Lifecycle.removeAndAddObserver(observer: LifecycleObserver) {
    removeObserver(observer)
    addObserver(observer)
}
