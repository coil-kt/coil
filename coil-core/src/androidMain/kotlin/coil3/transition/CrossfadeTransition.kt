package coil3.transition

import coil3.asCoilImage
import coil3.decode.DataSource
import coil3.request.DEFAULT_CROSSFADE_MILLIS
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult

/**
 * A [Transition] that crossfades from the current drawable to a new one.
 *
 * @param durationMillis The duration of the animation in milliseconds.
 * @param preferExactIntrinsicSize See [CrossfadeDrawable.preferExactIntrinsicSize].
 */
class CrossfadeTransition @JvmOverloads constructor(
    private val target: TransitionTarget,
    private val result: ImageResult,
    val durationMillis: Int = DEFAULT_CROSSFADE_MILLIS,
    val preferExactIntrinsicSize: Boolean = false,
) : Transition {

    init {
        require(durationMillis > 0) { "durationMillis must be > 0." }
    }

    override fun transition() {
        val drawable = CrossfadeDrawable(
            start = target.drawable,
            end = result.image?.asDrawable(target.view.resources),
            scale = result.request.scale,
            durationMillis = durationMillis,
            fadeStart = result !is SuccessResult || !result.isPlaceholderCached,
            preferExactIntrinsicSize = preferExactIntrinsicSize,
        )
        when (result) {
            is SuccessResult -> target.onSuccess(drawable.asCoilImage())
            is ErrorResult -> target.onError(drawable.asCoilImage())
        }
    }

    class Factory @JvmOverloads constructor(
        val durationMillis: Int = DEFAULT_CROSSFADE_MILLIS,
        val preferExactIntrinsicSize: Boolean = false,
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
    }
}
