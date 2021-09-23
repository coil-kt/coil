package coil.transition

import android.widget.ImageView
import coil.decode.DataSource
import coil.drawable.CrossfadeDrawable
import coil.request.ErrorResult
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.size.Scale
import coil.util.scale

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

    override fun transition() {
        val drawable = CrossfadeDrawable(
            start = target.drawable,
            end = result.drawable,
            scale = (target.view as? ImageView)?.scale ?: Scale.FIT,
            durationMillis = durationMillis,
            fadeStart = !(result is SuccessResult && result.isPlaceholderCached),
            preferExactIntrinsicSize = preferExactIntrinsicSize
        )
        when (result) {
            is SuccessResult -> target.onSuccess(drawable)
            is ErrorResult -> target.onError(drawable)
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
