package coil3.transition

import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult

/**
 * A transition that applies the [ImageResult] on the [TransitionTarget] without animating.
 */
internal class NoneTransition(
    private val target: TransitionTarget,
    private val result: ImageResult
) : Transition {

    override fun transition() {
        when (result) {
            is SuccessResult -> target.onSuccess(result.image)
            is ErrorResult -> target.onError(result.image)
        }
    }

    class Factory : Transition.Factory {

        override fun create(target: TransitionTarget, result: ImageResult): Transition {
            return NoneTransition(target, result)
        }
    }
}
