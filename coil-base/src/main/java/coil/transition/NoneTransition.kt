package coil.transition

import coil.request.ErrorResult
import coil.request.ImageResult
import coil.request.SuccessResult

/**
 * A transition that applies the [ImageResult] on the [TransitionTarget] without animating.
 */
internal class NoneTransition(
    private val target: TransitionTarget,
    private val result: ImageResult
) : Transition {

    override suspend fun transition() {
        when (result) {
            is SuccessResult -> target.onSuccess(result.drawable)
            is ErrorResult -> target.onError(result.drawable)
        }
    }

    class Factory : Transition.Factory {

        override fun create(target: TransitionTarget, result: ImageResult): Transition {
            return NoneTransition(target, result)
        }

        override fun equals(other: Any?) = other is Factory

        override fun hashCode() = javaClass.hashCode()
    }
}
