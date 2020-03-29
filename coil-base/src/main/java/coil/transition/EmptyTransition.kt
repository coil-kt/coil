@file:OptIn(ExperimentalCoilApi::class)

package coil.transition

import coil.annotation.ExperimentalCoilApi
import coil.transition.TransitionResult.Error
import coil.transition.TransitionResult.Success

/**
 * A transition that applies the [TransitionResult] on the [TransitionTarget] without animating.
 */
internal object EmptyTransition : Transition {

    override suspend fun transition(target: TransitionTarget<*>, result: TransitionResult) {
        when (result) {
            is Success -> target.onSuccess(result.drawable)
            is Error -> target.onError(result.drawable)
        }
    }
}
