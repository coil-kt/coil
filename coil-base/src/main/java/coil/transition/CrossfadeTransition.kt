package coil.transition

import android.graphics.drawable.Drawable
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import coil.annotation.ExperimentalCoil
import coil.drawable.CrossfadeDrawable
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** A [Transition] that crossfades from the current drawable to a new one. */
@ExperimentalCoil
class CrossfadeTransition(private val durationMillis: Int) : Transition {

    init {
        require(durationMillis > 0) { "durationMillis must be > 0." }
    }

    override suspend fun transition(
        adapter: Transition.Adapter,
        drawable: Drawable?
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        val crossfade = CrossfadeDrawable(
            start = adapter.drawable,
            end = drawable,
            scale = adapter.scale,
            durationMillis = durationMillis
        )
        val callback = Callback(crossfade, continuation)
        crossfade.registerAnimationCallback(callback)
        continuation.invokeOnCancellation(callback)
        adapter.drawable = crossfade
    }

    /** Handle cancellation of the continuation and completion of the animation in one object. */
    private class Callback(
        private val crossfade: CrossfadeDrawable,
        private val continuation: CancellableContinuation<Unit>
    ) : Animatable2Compat.AnimationCallback(), CompletionHandler {

        override fun onAnimationEnd(drawable: Drawable) {
            crossfade.unregisterAnimationCallback(this)
            continuation.resume(Unit)
        }

        override fun invoke(cause: Throwable?) = crossfade.stop()
    }

    class Factory(durationMillis: Int) : Transition.Factory {

        // CrossfadeTransition is stateless so we can reuse the same instance.
        private val transition = CrossfadeTransition(durationMillis)

        override fun newTransition(event: Transition.Event): Transition? {
            return transition.takeIf { event != Transition.Event.CACHED }
        }
    }
}
