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
     *
     * @param target The target to apply this transition to.
     * @param result The result of the image request.
     */
    @MainThread
    suspend fun transition(target: TransitionTarget, result: ImageResult)

    /** A factory that creates new [Transition] instances. */
    fun interface Factory {

        /** Create a new [Transition]. */
        fun create(target: TransitionTarget, result: ImageResult): Transition
    }

    companion object {
        @JvmField val NONE: Transition = NoneTransition()
    }
}
