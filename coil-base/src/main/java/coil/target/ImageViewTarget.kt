@file:OptIn(ExperimentalCoilApi::class)

package coil.target

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import coil.annotation.ExperimentalCoilApi
import coil.transition.TransitionTarget

/** A [Target] that handles setting images on an [ImageView]. */
open class ImageViewTarget(
    override val view: ImageView
) : PoolableViewTarget<ImageView>, TransitionTarget<ImageView>, DefaultLifecycleObserver {

    private var isStarted = false

    override val drawable: Drawable?
        get() = view.drawable

    override fun onStart(placeholder: Drawable?) = setDrawable(placeholder)

    override fun onSuccess(result: Drawable) = setDrawable(result)

    override fun onError(error: Drawable?) = setDrawable(error)

    override fun onClear() = setDrawable(null)

    override fun onStart(owner: LifecycleOwner) {
        isStarted = true
        updateAnimation()
    }

    override fun onStop(owner: LifecycleOwner) {
        isStarted = false
        updateAnimation()
    }

    /** Replace the [ImageView]'s current drawable with [drawable]. */
    protected open fun setDrawable(drawable: Drawable?) {
        (view.drawable as? Animatable)?.stop()
        view.setImageDrawable(drawable)
        updateAnimation()
    }

    /** Start/stop the current [Drawable]'s animation based on the current lifecycle state. */
    protected open fun updateAnimation() {
        val animatable = view.drawable as? Animatable ?: return
        if (isStarted) animatable.start() else animatable.stop()
    }
}
