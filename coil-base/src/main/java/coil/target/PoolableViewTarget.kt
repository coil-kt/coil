package coil.target

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.MainThread

/**
 * A [ViewTarget] that supports [Bitmap] pooling.
 *
 * Implementing [PoolableViewTarget] opts this target into bitmap pooling. This allows Coil to re-use [Bitmap]s
 * given to this target, which can conserve memory by avoiding a new [Bitmap] allocation.
 *
 * To opt out of bitmap pooling, implement [ViewTarget] instead.
 *
 * Implementing [PoolableViewTarget] requires that you must stop using the previous [Drawable] as soon as
 * the next [PoolableViewTarget] lifecycle method is called; one of:
 *
 * [Target.onStart], [Target.onSuccess], [Target.onError], [PoolableViewTarget.onClear].
 *
 * For example, a [PoolableViewTarget] must stop using the placeholder drawable from [Target.onStart]
 * as soon as [Target.onSuccess] is called.
 *
 * Continuing to use the previous [Drawable] after the next lifecycle method is called can cause rendering issues
 * and/or throw exceptions.
 *
 * @see ViewTarget
 * @see ImageViewTarget
 */
interface PoolableViewTarget<T : View> : ViewTarget<T> {

    /**
     * Called when the current drawable is no longer usable. Targets **must** stop using the current Drawable.
     *
     * In practice, this will only be called when the view is detached or about to be destroyed.
     */
    @MainThread
    fun onClear()
}
