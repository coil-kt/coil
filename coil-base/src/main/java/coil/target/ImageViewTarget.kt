package coil.target

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import coil.size.Scale
import coil.transition.Transition
import coil.util.scale

/** A [Target] that handles setting images on an [ImageView]. */
open class ImageViewTarget(
    override val view: ImageView
) : PoolableViewTarget<ImageView>, DefaultLifecycleObserver, Transition.Adapter {

    private var isStarted = false

    override var drawable: Drawable?
        get() = view.drawable
        set(value) = setDrawableInternal(value)

    override val scale: Scale
        get() = view.scale

    override fun onStart(placeholder: Drawable?) = setDrawableInternal(placeholder)

    override fun onSuccess(result: Drawable) = setDrawableInternal(result)

    override fun onError(error: Drawable?) = setDrawableInternal(error)

    override fun onClear() = setDrawableInternal(null)

    override fun onStart(owner: LifecycleOwner) {
        isStarted = true
        updateAnimation()
    }

    override fun onStop(owner: LifecycleOwner) {
        isStarted = false
        updateAnimation()
    }

    /** Set [drawable] to this [ImageView]. */
    protected open fun setDrawableInternal(drawable: Drawable?) {
        (view.drawable as? Animatable)?.stop()
        view.setImageDrawable(drawable)
        updateAnimation()
    }

    /** Start/stop the current [Drawable]'s animation based on the lifecycle state. */
    protected open fun updateAnimation() {
        val animatable = view.drawable as? Animatable ?: return
        if (isStarted) animatable.start() else animatable.stop()
    }
}
