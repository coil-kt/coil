package coil.transition

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import coil.decode.DataSource
import coil.drawable.CrossfadeDrawable
import coil.request.ErrorResult
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.size.Scale
import coil.util.scale
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * A [Transition] that crossfades from the current drawable to a new one.
 *
 * @param durationMillis The duration of the animation in milliseconds.
 * @param preferExactIntrinsicSize See [CrossfadeDrawable.preferExactIntrinsicSize].
 */
class CrossfadeTransition @JvmOverloads constructor(
    private val target: TransitionTarget,
    private val result: ImageResult,
    val durationMillis: Int = CrossfadeDrawable.DEFAULT_DURATION,
    val preferExactIntrinsicSize: Boolean = false
) : Transition {

    init {
        require(durationMillis > 0) { "durationMillis must be > 0." }
    }

    override suspend fun transition() {
        // Animate the drawable and suspend until the animation completes.
        var outerCrossfade: CrossfadeDrawable? = null
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                val crossfade = CrossfadeDrawable(
                    start = target.drawable,
                    end = result.drawable,
                    scale = (target.view as? ImageView)?.scale ?: Scale.FILL,
                    durationMillis = durationMillis,
                    fadeStart = (result as? SuccessResult)?.placeholderMemoryCacheKey == null,
                    preferExactIntrinsicSize = preferExactIntrinsicSize
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

    class Factory @JvmOverloads constructor(
        val durationMillis: Int = CrossfadeDrawable.DEFAULT_DURATION,
        val preferExactIntrinsicSize: Boolean = false
    ) : Transition.Factory {

        init {
            require(durationMillis > 0) { "durationMillis must be > 0." }
        }

        override fun create(target: TransitionTarget, result: ImageResult): Transition {
            // Only animate successful requests.
            if (result !is SuccessResult) {
                return Transition.Factory.NONE.create(target, result)
            }

            // Don't animate if the request was fulfilled by the memory cache.
            if (result.dataSource == DataSource.MEMORY_CACHE) {
                return Transition.Factory.NONE.create(target, result)
            }

            // Don't animate if the view is not visible as 'CrossfadeDrawable.onDraw'
            // won't be called until the view becomes visible.
            if (!target.view.isVisible) {
                return Transition.Factory.NONE.create(target, result)
            }

            return CrossfadeTransition(target, result, durationMillis, preferExactIntrinsicSize)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Factory &&
                durationMillis == other.durationMillis &&
                preferExactIntrinsicSize == other.preferExactIntrinsicSize
        }

        override fun hashCode(): Int {
            var result = durationMillis
            result = 31 * result + preferExactIntrinsicSize.hashCode()
            return result
        }
    }
}
