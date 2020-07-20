package coil.transition

import coil.annotation.ExperimentalCoilApi
import coil.request.ErrorResult
import coil.request.RequestResult
import coil.request.SuccessResult

/**
 * A transition that applies the [RequestResult] on the [TransitionTarget] without animating.
 */
@ExperimentalCoilApi
internal object NoneTransition : Transition {

    override suspend fun transition(target: TransitionTarget<*>, result: RequestResult) {
        when (result) {
            is SuccessResult -> target.onSuccess(result.drawable)
            is ErrorResult -> target.onError(result.drawable)
        }
    }

    override fun toString() = "coil.transition.NoneTransition"
}
