package coil.transition

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.MainThread
import coil.ImageLoader
import coil.size.Scale
import coil.target.Target

/**
 * A class to animate between a [Target]'s current drawable and the result of an image request.
 *
 * NOTE: A [Target] must implement [Transition.Adapter] or any [Transition]s for that [Target] will be ignored.
 */
interface Transition {

    /**
     * Start the transition animation and suspend until it completes or is cancelled.
     *
     * Failure to suspend until the animation is complete can cause the [drawable]'s [Bitmap] (if any)
     * to be pooled while it is still in use. See [CrossfadeTransition] for an example.
     *
     * @param adapter The adapter to apply this transition to.
     * @param drawable The drawable to transition to.
     */
    @MainThread
    suspend fun transition(adapter: Adapter, drawable: Drawable)

    /** A [Target] that supports applying [Transition]s. */
    interface Adapter {

        /** The underlying [View] that this [Transition] is being applied to. */
        val view: View

        /** The [view]'s current scaling algorithm. */
        val scale: Scale
            get() = Scale.FILL

        /** The [view]'s current [Drawable]. */
        var drawable: Drawable?
    }

    /** A class that creates new [Transition]s depending on the result of an image request. */
    interface Factory {

        /**
         * Return a [Transition] to be applied to the [Transition.Adapter].
         *
         * @param isMemoryCache True if the image was retrieved from the [ImageLoader]'s memory cache.
         * @return Return the [Transition] to be applied to the [Transition.Adapter]. Return null to perform no transition.
         */
        fun newTransition(isMemoryCache: Boolean): Transition?
    }
}
