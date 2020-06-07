@file:JvmName("-Lifecycles")
@file:Suppress("NOTHING_TO_INLINE")

package coil.util

import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Suspend until [Lifecycle.getCurrentState] is at least [STARTED] */
@MainThread
internal suspend inline fun Lifecycle.awaitStarted() {
    // Fast path: we're already started.
    if (currentState.isAtLeast(STARTED)) return

    // Slow path: observe the lifecycle until we're started.
    var observer: LifecycleObserver? = null
    try {
        suspendCancellableCoroutine<Unit> { continuation ->
            observer = object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    removeObserver(this)
                    continuation.resume(Unit)
                }
            }
            addObserver(observer!!)
        }
    } catch (throwable: Throwable) {
        // Ensure exceptions are handled on the main thread.
        observer?.let(::removeObserver)
        throw throwable
    }
}
