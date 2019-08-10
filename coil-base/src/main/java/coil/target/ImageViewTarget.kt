package coil.target

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import coil.drawable.CrossfadeDrawable
import coil.request.Request
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * A [Target], which handles setting images on an [ImageView].
 */
class ImageViewTarget(override val view: ImageView) : PoolableViewTarget<ImageView>, DefaultLifecycleObserver {

    private var isStarted = false

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

    /**
     * Internal method to crossfade the successful [Drawable] with the current drawable.
     *
     * This is called instead of [onSuccess] if [Request.crossfadeMillis] > 0.
     *
     * The request is suspended until the animation is complete to avoid pooling the
     * current drawable while it is still being used by the animation.
     */
    internal suspend inline fun onSuccessCrossfade(
        result: Drawable,
        duration: Int
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        val drawable = CrossfadeDrawable(
            start = view.drawable,
            end = result,
            duration = duration,
            onEnd = { continuation.resume(Unit) }
        )
        continuation.invokeOnCancellation { drawable.stop() }
        onSuccess(drawable)
    }

    private fun setDrawable(drawable: Drawable?) {
        (view.drawable as? Animatable)?.stop()
        view.setImageDrawable(drawable)
        updateAnimation()
    }

    private fun updateAnimation() {
        val animatable = view.drawable as? Animatable ?: return
        if (isStarted) animatable.start() else animatable.stop()
    }
}
