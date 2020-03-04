@file:OptIn(ExperimentalCoilApi::class)

package coil.memory

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.MainThread
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.base.R
import coil.request.Request
import coil.target.PoolableViewTarget
import coil.target.Target
import coil.transition.Transition
import coil.transition.TransitionResult.Error
import coil.transition.TransitionResult.Success
import coil.transition.TransitionTarget
import coil.util.log

/**
 * Wrap a [Target] to support [Bitmap] pooling.
 *
 * @see DelegateService
 */
internal sealed class TargetDelegate {

    companion object {
        const val TAG = "TargetDelegate"
    }

    @MainThread
    open fun start(cached: BitmapDrawable?, placeholder: Drawable?) {}

    @MainThread
    open suspend fun success(result: Drawable, isMemoryCache: Boolean, transition: Transition?) {}

    @MainThread
    open suspend fun error(error: Drawable?, transition: Transition?) {}

    @MainThread
    open fun clear() {}
}

/**
 * An empty target delegate. Used if the request has no target and does not need to invalidate bitmaps.
 */
internal object EmptyTargetDelegate : TargetDelegate()

/**
 * Only invalidate the success bitmaps.
 *
 * Used if [Request.target] is null and the success [Drawable] is leaked.
 *
 * @see ImageLoader.get
 */
internal class InvalidatableEmptyTargetDelegate(
    override val referenceCounter: BitmapReferenceCounter
) : TargetDelegate(), Invalidatable {

    override suspend fun success(result: Drawable, isMemoryCache: Boolean, transition: Transition?) {
        invalidate(result.bitmap)
    }
}

/**
 * Invalidate the cached bitmap and the success bitmap.
 */
internal class InvalidatableTargetDelegate(
    val target: Target,
    override val referenceCounter: BitmapReferenceCounter
) : TargetDelegate(), Invalidatable {

    override fun start(cached: BitmapDrawable?, placeholder: Drawable?) {
        invalidate(cached?.bitmap)
        target.onStart(placeholder)
    }

    override suspend fun success(result: Drawable, isMemoryCache: Boolean, transition: Transition?) {
        invalidate(result.bitmap)
        target.onSuccess(result, isMemoryCache, transition)
    }

    override suspend fun error(error: Drawable?, transition: Transition?) {
        target.onError(error, transition)
    }
}

/**
 * Handle the reference counts for the cached bitmap and the success bitmap.
 */
internal class PoolableTargetDelegate(
    override val target: PoolableViewTarget<*>,
    override val referenceCounter: BitmapReferenceCounter
) : TargetDelegate(), Poolable {

    override fun start(cached: BitmapDrawable?, placeholder: Drawable?) {
        instrument(cached?.bitmap) { onStart(placeholder) }
    }

    override suspend fun success(result: Drawable, isMemoryCache: Boolean, transition: Transition?) {
        instrument(result.bitmap) { onSuccess(result, isMemoryCache, transition) }
    }

    override suspend fun error(error: Drawable?, transition: Transition?) {
        instrument(null) { onError(error, transition) }
    }

    override fun clear() {
        instrument(null) { onClear() }
    }
}

private interface Invalidatable {

    val referenceCounter: BitmapReferenceCounter

    fun invalidate(bitmap: Bitmap?) {
        bitmap?.let(referenceCounter::invalidate)
    }
}

private interface Poolable {

    private inline var PoolableViewTarget<*>.bitmap: Bitmap?
        get() = view.getTag(R.id.coil_bitmap) as? Bitmap
        set(value) = view.setTag(R.id.coil_bitmap, value)

    val target: PoolableViewTarget<*>
    val referenceCounter: BitmapReferenceCounter

    /** Increment the reference counter for the current Bitmap. */
    fun increment(bitmap: Bitmap?) {
        bitmap?.let(referenceCounter::increment)
    }

    /** Replace the reference to the currently cached Bitmap. */
    fun decrement(bitmap: Bitmap?) {
        target.bitmap?.let(referenceCounter::decrement)
        target.bitmap = bitmap
    }
}

private inline val Drawable.bitmap: Bitmap?
    get() = (this as? BitmapDrawable)?.bitmap

private inline fun Poolable.instrument(bitmap: Bitmap?, update: PoolableViewTarget<*>.() -> Unit) {
    increment(bitmap)
    target.update()
    decrement(bitmap)
}

private suspend inline fun Target.onSuccess(result: Drawable, isMemoryCache: Boolean, transition: Transition?) {
    if (transition == null) {
        onSuccess(result)
        return
    }

    if (this !is TransitionTarget<*>) {
        log(TargetDelegate.TAG, Log.WARN) {
            "Ignoring '$transition' as '$this' does not implement coil.transition.TransitionTarget."
        }
        onSuccess(result)
        return
    }

    transition.transition(this, Success(result, isMemoryCache))
}

private suspend inline fun Target.onError(error: Drawable?, transition: Transition?) {
    if (transition == null) {
        onError(error)
        return
    }

    if (this !is TransitionTarget<*>) {
        log(TargetDelegate.TAG, Log.WARN) {
            "Ignoring '$transition' as '$this' does not implement coil.transition.TransitionTarget."
        }
        onError(error)
        return
    }

    transition.transition(this, Error(error))
}
