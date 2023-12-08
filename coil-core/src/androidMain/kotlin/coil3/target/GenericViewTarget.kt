package coil3.target

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import coil3.Image
import coil3.transition.TransitionTarget

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

    override fun onStart(placeholder: Image?) = updateImage(placeholder)

    override fun onError(error: Image?) = updateImage(error)

    override fun onSuccess(result: Image) = updateImage(result)

    override fun onStart(owner: LifecycleOwner) {
        isStarted = true
        updateAnimation()
    }

    override fun onStop(owner: LifecycleOwner) {
        isStarted = false
        updateAnimation()
    }

    /** Replace the [ImageView]'s current image with [image]. */
    protected fun updateImage(image: Image?) {
        val drawable = image?.asDrawable(view.resources)
        (this.drawable as? Animatable)?.stop()
        this.drawable = drawable
        updateAnimation()
    }

    /** Start/stop the current [Drawable]'s animation based on the current lifecycle state. */
    protected fun updateAnimation() {
        val animatable = drawable as? Animatable ?: return
        if (isStarted) animatable.start() else animatable.stop()
    }
}
