package coil.transition

import android.graphics.drawable.Drawable
import android.view.View
import coil.annotation.ExperimentalCoilApi
import coil.target.Target

/**
 * A [Target] that supports applying [Transition]s.
 */
@ExperimentalCoilApi
public interface TransitionTarget : Target {

    /**
     * The [View] used by this [Target].
     */
    public val view: View

    /**
     * The [view]'s current [Drawable].
     */
    public val drawable: Drawable?
}
