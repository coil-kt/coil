package coil.memory

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.MainThread
import coil.ImageLoader
import coil.base.R
import coil.drawable.CrossfadeDrawable
import coil.request.Request
import coil.target.ImageViewTarget
import coil.target.PoolableViewTarget
import coil.target.Target

/**
 * Wrap a [Target] to support [Bitmap] pooling.
 *
 * @see DelegateService
 */
internal sealed class TargetDelegate {

    @MainThread
    open fun start(cached: BitmapDrawable?, placeholder: Drawable?) {}

    @MainThread
    open suspend fun success(result: Drawable, crossfadeMillis: Int) {}

    @MainThread
    open fun error(error: Drawable?, crossfadeMillis: Int) {}

    @MainThread
    open fun clear() {}
}

/**
 * An empty target delegate. Used if the request has no target and does not need to invalidate Bitmaps.
 */
internal object EmptyTargetDelegate : TargetDelegate()

/**
 * Only invalidate the success Bitmap.
 *
 * Used if [Request.target] is null and the success [Drawable] is leaked.
 *
 * @see ImageLoader.get
 */
internal class InvalidatableEmptyTargetDelegate(
    override val referenceCounter: BitmapReferenceCounter
) : TargetDelegate(), Invalidable {

    override suspend fun success(result: Drawable, crossfadeMillis: Int) {
        invalidate(result.bitmap)
    }
}

/**
 * Invalidate the cached Bitmap and the success Bitmap.
 */
internal class InvalidatableTargetDelegate(
    val target: Target,
    override val referenceCounter: BitmapReferenceCounter
) : TargetDelegate(), Invalidable {

    override fun start(cached: BitmapDrawable?, placeholder: Drawable?) {
        invalidate(cached?.bitmap)
        target.onStart(placeholder)
    }

    override suspend fun success(result: Drawable, crossfadeMillis: Int) {
        invalidate(result.bitmap)
        target.onSuccess(result)
    }

    override fun error(error: Drawable?, crossfadeMillis: Int) {
        target.onError(error)
    }
}

/**
 * Handle the reference counts for the cached Bitmap and the success Bitmap.
 */
internal class PoolableTargetDelegate(
    override val target: PoolableViewTarget<*>,
    override val referenceCounter: BitmapReferenceCounter
) : TargetDelegate(), Poolable {

    override fun start(cached: BitmapDrawable?, placeholder: Drawable?) {
        instrument(cached?.bitmap) { onStart(placeholder) }
    }

    override suspend fun success(result: Drawable, crossfadeMillis: Int) {
        instrument(result.bitmap) { onSuccess(result, crossfadeMillis) }
    }

    override fun error(error: Drawable?, crossfadeMillis: Int) {
        instrument(null) { onError(error, crossfadeMillis) }
    }

    override fun clear() {
        instrument(null) { onClear() }
    }
}

private interface Invalidable {

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

    /**
     * Increment the reference counter for the current Bitmap.
     */
    fun increment(bitmap: Bitmap?) {
        bitmap?.let(referenceCounter::increment)
    }

    /**
     * Replace the reference to the currently cached Bitmap.
     */
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

private suspend inline fun Poolable.onSuccess(result: Drawable, crossfadeMillis: Int) {
    val target = target
    if (crossfadeMillis > 0 && target is ImageViewTarget) {
        target.onSuccessCrossfade(result, crossfadeMillis)
    } else {
        target.onSuccess(result)
    }
}

private fun Poolable.onError(error: Drawable?, crossfadeMillis: Int) {
    val target = target
    if (crossfadeMillis > 0 && error != null && target is ImageViewTarget) {
        target.onError(CrossfadeDrawable(target.view.drawable, error))
    } else {
        target.onError(error)
    }
}
