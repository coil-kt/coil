package coil.target

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import coil.transition.TransitionTarget

/**
 * An opinionated [ViewTarget] that simplifies updating the [Drawable] attached to a [View]
 * and supports automatically starting and stopping animated [Drawable]s.
 *
 * If you need custom behaviour that this class doesn't support it's recommended
 * to implement [ViewTarget] directly.
 */
abstract class GenericViewTarget<T : View> : ViewTarget<T>, TransitionTarget, DefaultLifecycleObserver {

    private var isStarted = false

    /**
     * The current [Drawable] attached to [view].
     */
    abstract override var drawable: Drawable?

    override fun onStart(placeholder: Drawable?) = updateDrawable(placeholder)

    override fun onError(error: Drawable?) = updateDrawable(error)

    override fun onSuccess(result: Drawable) = updateDrawable(result)

    override fun onStart(owner: LifecycleOwner) {
        isStarted = true
        updateAnimation()
    }

    override fun onStop(owner: LifecycleOwner) {
        isStarted = false
        updateAnimation()
    }

    /** Replace the [ImageView]'s current drawable with [drawable]. */
    private fun updateDrawable(drawable: Drawable?) {
        (this.drawable as? Animatable)?.stop()
        this.drawable = drawable
        updateAnimation()
    }

    /** Start/stop the current [Drawable]'s animation based on the current lifecycle state. */
    private fun updateAnimation() {
        val animatable = drawable as? Animatable ?: return
        if (isStarted) animatable.start() else animatable.stop()
    }
}
