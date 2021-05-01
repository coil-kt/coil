package coil.transition

import coil.request.ErrorResult
import coil.request.ImageResult
import coil.request.SuccessResult

/**
 * A transition that applies the [ImageResult] on the [TransitionTarget] without animating.
 */
internal class NoneTransition : Transition {

    override suspend fun transition(target: TransitionTarget, result: ImageResult) {
        when (result) {
            is SuccessResult -> target.onSuccess(result.drawable)
            is ErrorResult -> target.onError(result.drawable)
        }
    }
}
