package coil.transition

import android.graphics.drawable.Drawable
import android.view.View
import coil.annotation.ExperimentalCoil
import coil.target.Target
import coil.target.ViewTarget

/**
 * A [Target] that supports applying [Transition]s.
 */
@ExperimentalCoil
interface TransitionTarget<T : View> : ViewTarget<T> {

    /**
     * The [view]'s current [Drawable].
     */
    val drawable: Drawable?
}
