@file:JvmName("-Lifecycles")
@file:Suppress("NOTHING_TO_INLINE")

package coil.util

import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Suspend until [this] is at least [STARTED] */
@MainThread
internal suspend inline fun Lifecycle.awaitStarted() {
    // Fast path: we're already started.
    if (currentState.isAtLeast(STARTED)) return

    // Slow path: observe the lifecycle until we're started.
    suspendCancellableCoroutine<Unit> { continuation ->
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                removeObserver(this)
                continuation.resume(Unit)
            }
        }
        continuation.invokeOnCancellation {
            removeObserver(observer)
        }
        addObserver(observer)
    }
}
