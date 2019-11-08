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
class ImageViewTarget(
    override val view: ImageView
) : PoolableViewTarget<ImageView>, DefaultLifecycleObserver, Transition.Adapter {

    private var isStarted = false

    override var drawable: Drawable?
        get() = view.drawable
        set(value) = applyDrawable(value)

    override val scale: Scale
        get() = view.scale

    override fun onStart(placeholder: Drawable?) = applyDrawable(placeholder)

    override fun onSuccess(result: Drawable) = applyDrawable(result)

    override fun onError(error: Drawable?) = applyDrawable(error)

    override fun onClear() = applyDrawable(null)

    override fun onStart(owner: LifecycleOwner) {
        isStarted = true
        updateAnimation()
    }

    override fun onStop(owner: LifecycleOwner) {
        isStarted = false
        updateAnimation()
    }

    private fun applyDrawable(drawable: Drawable?) {
        (view.drawable as? Animatable)?.stop()
        view.setImageDrawable(drawable)
        updateAnimation()
    }

    private fun updateAnimation() {
        val animatable = view.drawable as? Animatable ?: return
        if (isStarted) animatable.start() else animatable.stop()
    }
}
