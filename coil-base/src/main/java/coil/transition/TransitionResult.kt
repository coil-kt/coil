package coil.transition

import android.graphics.drawable.Drawable
import coil.annotation.ExperimentalCoilApi
import coil.target.Target

/**
 * Represents the result of an image request.
 */
@ExperimentalCoilApi
sealed class TransitionResult {

    /**
     * Represents a *successful* image request.
     *
     * When passed to [Transition.transition], implementations should call [Target.onSuccess] exactly once.
     *
     * @param drawable The [Drawable] to transition to.
     * @param isMemoryCache True if the request was fulfilled from the memory cache.
     */
    class Success(
        val drawable: Drawable,
        val isMemoryCache: Boolean
    ) : TransitionResult()

    /**
     * Represents a *failed* image request.
     *
     * When passed to [Transition.transition], implementations should call [Target.onError] exactly once.
     *
     * @param drawable The [Drawable] to transition to.
     */
    class Error(
        val drawable: Drawable?
    ) : TransitionResult()
}
