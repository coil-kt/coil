package coil.transition

import androidx.annotation.MainThread
import coil.request.ImageResult
import coil.target.Target

/**
 * A class to animate between a [Target]'s current drawable and the result of an image request.
 *
 * NOTE: A [Target] must implement [TransitionTarget] to support applying [Transition]s.
 * If the [Target] does not implement [TransitionTarget], any [Transition]s will be ignored.
 */
fun interface Transition {

    /**
     * Start the transition animation and suspend until it completes or is cancelled.
     *
     * Implementations are responsible for calling the correct [Target] lifecycle callback.
     * See [CrossfadeTransition] for an example.
     */
    @MainThread
    suspend fun transition()

    fun interface Factory {

        fun create(target: TransitionTarget, result: ImageResult): Transition

        companion object {
            @JvmField val NONE: Factory = NoneTransition.Factory()
        }
    }
}
