package coil.target

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.MainThread

/**
 * A [Target] that supports [Bitmap] pooling.
 *
 * Implementing [PoolableTarget] opts this target into bitmap pooling. This allows the image loader to re-use [Bitmap]s
 * given to this target, which improves performance.
 *
 * To opt out of bitmap pooling, implement [Target] instead.
 *
 * Implementing [PoolableTarget] requires that you must stop using the previous [Drawable] as soon as the next
 * [PoolableTarget] lifecycle method is called; one of:
 *
 * [Target.onStart], [Target.onSuccess], [Target.onError], [PoolableTarget.onClear].
 *
 * For example, a [PoolableTarget] must stop using the placeholder drawable from [Target.onStart]
 * as soon as [Target.onSuccess] is called.
 *
 * Continuing to use the previous [Drawable] after the next lifecycle method is called can cause rendering issues
 * and/or throw exceptions.
 */
interface PoolableTarget : Target {

    /**
     * The current bitmap attached to this target.
     *
     * Implementations should store and return the same value irrespective of the
     * drawable passed to [onStart], [onSuccess], or [onError].
     */
    var bitmap: Bitmap?

    /**
     * Called when the current drawable is no longer usable. Targets **must** stop using the current drawable.
     */
    @MainThread
    fun onClear()
}
