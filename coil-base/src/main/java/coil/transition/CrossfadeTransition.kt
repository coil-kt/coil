package coil.transition

import android.graphics.drawable.Drawable
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import coil.drawable.CrossfadeDrawable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** A [Transition] that crossfades from the current drawable to a new one. */
class CrossfadeTransition(private val durationMillis: Int) : Transition {

    init {
        require(durationMillis > 0) { "durationMillis must be > 0." }
    }

    override suspend fun transition(
        adapter: Transition.Adapter,
        drawable: Drawable
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        val crossfadeDrawable = CrossfadeDrawable(
            start = adapter.drawable,
            end = drawable,
            scale = adapter.scale,
            durationMillis = durationMillis
        )
        crossfadeDrawable.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable) {
                crossfadeDrawable.unregisterAnimationCallback(this)
                continuation.resume(Unit)
            }
        })
        continuation.invokeOnCancellation { crossfadeDrawable.stop() }
        adapter.drawable = crossfadeDrawable
    }

    class Factory(durationMillis: Int) : Transition.Factory {

        // CrossfadeTransition is stateless so we can reuse the same instance.
        private val transition = CrossfadeTransition(durationMillis)

        override fun newTransition(isMemoryCache: Boolean) = transition.takeUnless { isMemoryCache }
    }
}
