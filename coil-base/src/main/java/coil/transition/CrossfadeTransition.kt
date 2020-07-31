package coil.transition

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.drawable.CrossfadeDrawable
import coil.request.ErrorResult
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.size.Scale
import coil.util.scale
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** A [Transition] that crossfades from the current drawable to a new one. */
@ExperimentalCoilApi
class CrossfadeTransition @JvmOverloads constructor(
    val durationMillis: Int = CrossfadeDrawable.DEFAULT_DURATION
) : Transition {

    init {
        require(durationMillis > 0) { "durationMillis must be > 0." }
    }

    override suspend fun transition(target: TransitionTarget, result: ImageResult) {
        // Don't animate if the request was fulfilled by the memory cache.
        if (result is SuccessResult && result.metadata.dataSource == DataSource.MEMORY_CACHE) {
            target.onSuccess(result.drawable)
            return
        }

        // Don't animate if the view is not visible as CrossfadeDrawable.onDraw
        // won't be called until the view becomes visible.
        if (!target.view.isVisible) {
            when (result) {
                is SuccessResult -> target.onSuccess(result.drawable)
                is ErrorResult -> target.onError(result.drawable)
            }
            return
        }

        // Animate the drawable and suspend until the animation completes.
        var outerCrossfade: CrossfadeDrawable? = null
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                val crossfade = CrossfadeDrawable(
                    start = target.drawable,
                    end = result.drawable,
                    scale = (target.view as? ImageView)?.scale ?: Scale.FILL,
                    durationMillis = durationMillis,
                    fadeStart = result !is SuccessResult || !result.metadata.isPlaceholderMemoryCacheKeyPresent
                )
                outerCrossfade = crossfade
                crossfade.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable?) {
                        crossfade.unregisterAnimationCallback(this)
                        continuation.resume(Unit)
                    }
                })
                when (result) {
                    is SuccessResult -> target.onSuccess(crossfade)
                    is ErrorResult -> target.onError(crossfade)
                }
            }
        } catch (throwable: Throwable) {
            // Ensure exceptions are handled on the main thread.
            outerCrossfade?.stop()
            throw throwable
        }
    }

    override fun equals(other: Any?): Boolean {
        return (this === other) || (other is CrossfadeTransition && durationMillis == other.durationMillis)
    }

    override fun hashCode() = durationMillis.hashCode()

    override fun toString() = "CrossfadeTransition(durationMillis=$durationMillis)"
}
