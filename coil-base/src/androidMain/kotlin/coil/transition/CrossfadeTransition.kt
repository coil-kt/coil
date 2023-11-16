package coil.transition

import coil.asCoilImage
import coil.decode.DataSource
import coil.drawable
import coil.request.ErrorResult
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.util.DEFAULT_CROSSFADE_MILLIS
import dev.drewhamilton.poko.Poko

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
    val preferExactIntrinsicSize: Boolean = false
) : Transition {

    init {
        require(durationMillis > 0) { "durationMillis must be > 0." }
    }

    override fun transition() {
        val drawable = CrossfadeDrawable(
            start = target.drawable,
            end = result.image?.drawable,
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

    @Poko
    class Factory @JvmOverloads constructor(
        val durationMillis: Int = DEFAULT_CROSSFADE_MILLIS,
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
    }
}
