@file:UseExperimental(ExperimentalCoil::class)

package coil.target

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import coil.annotation.ExperimentalCoil
import coil.transition.Transition

/** A [Target] that handles setting images on an [ImageView]. */
open class ImageViewTarget(
    override val view: ImageView
) : PoolableViewTarget<ImageView>, DefaultLifecycleObserver, Transition.Adapter {

    private var isStarted = false

    override var drawable: Drawable?
        get() = view.drawable
        set(value) = updateDrawable(value)

    override fun onStart(placeholder: Drawable?) = updateDrawable(placeholder)

    override fun onSuccess(result: Drawable) = updateDrawable(result)

    override fun onError(error: Drawable?) = updateDrawable(error)

    override fun onClear() = updateDrawable(null)

    override fun onStart(owner: LifecycleOwner) {
        isStarted = true
        updateAnimation()
    }

    override fun onStop(owner: LifecycleOwner) {
        isStarted = false
        updateAnimation()
    }

    /** Replace the [ImageView]'s current drawable with [drawable]. */
    protected open fun updateDrawable(drawable: Drawable?) {
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
