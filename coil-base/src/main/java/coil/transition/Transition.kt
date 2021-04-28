package coil.transition

import android.graphics.Bitmap
import androidx.annotation.MainThread
import coil.annotation.ExperimentalCoilApi
import coil.request.ImageResult
import coil.target.Target

/**
 * A class to animate between a [Target]'s current drawable and the result of an image request.
 *
 * NOTE: A [Target] must implement [TransitionTarget] to support applying [Transition]s.
 * If the [Target] does not implement [TransitionTarget], any [Transition]s will be ignored.
 */
@ExperimentalCoilApi
fun interface Transition {

    /**
     * Start the transition animation and suspend until it completes or is cancelled.
     *
     * Failure to suspend until the animation is complete can cause the [drawable]'s [Bitmap] (if any)
     * to be pooled while it is still in use.
     *
     * NOTE: Implementations are responsible for calling the correct [Target] lifecycle callback.
     * See [CrossfadeTransition] for an example.
     *
     * @param target The target to apply this transition to.
     * @param result The result of the image request.
     */
    @MainThread
    suspend fun transition(target: TransitionTarget, result: ImageResult)

    companion object {
        @JvmField val NONE: Transition = NoneTransition
    }
}
