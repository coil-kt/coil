@file:OptIn(ExperimentalCoilApi::class)

package coil.memory

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.MainThread
import coil.EventListener
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.base.R
import coil.request.ErrorResult
import coil.request.Request
import coil.request.RequestResult
import coil.request.SuccessResult
import coil.target.PoolableViewTarget
import coil.target.Target
import coil.transition.Transition
import coil.transition.TransitionTarget
import coil.util.Logger
import coil.util.log

/**
 * Wrap a [Target] to support [Bitmap] pooling.
 *
 * @see DelegateService
 */
internal sealed class TargetDelegate {

    @MainThread
    open fun start(cached: BitmapDrawable?, placeholder: Drawable?) {}

    @MainThread
    open suspend fun success(result: SuccessResult, transition: Transition) {}

    @MainThread
    open suspend fun error(result: ErrorResult, transition: Transition) {}

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

    override suspend fun success(result: SuccessResult, transition: Transition) {
        invalidate(result.bitmap)
    }
}

/**
 * Invalidate the cached bitmap and the success bitmap.
 */
internal class InvalidatableTargetDelegate(
    private val request: Request,
    private val target: Target,
    override val referenceCounter: BitmapReferenceCounter,
    private val eventListener: EventListener,
    private val logger: Logger?
) : TargetDelegate(), Invalidatable {

    override fun start(cached: BitmapDrawable?, placeholder: Drawable?) {
        invalidate(cached?.bitmap)
        target.onStart(placeholder)
    }

    override suspend fun success(result: SuccessResult, transition: Transition) {
        invalidate(result.bitmap)
        target.onSuccess(request, result, transition, eventListener, logger)
    }

    override suspend fun error(result: ErrorResult, transition: Transition) {
        target.onError(request, result, transition, eventListener, logger)
    }
}

/**
 * Handle the reference counts for the cached bitmap and the success bitmap.
 */
internal class PoolableTargetDelegate(
    private val request: Request,
    override val target: PoolableViewTarget<*>,
    override val referenceCounter: BitmapReferenceCounter,
    private val eventListener: EventListener,
    private val logger: Logger?
) : TargetDelegate(), Poolable {

    override fun start(cached: BitmapDrawable?, placeholder: Drawable?) {
        instrument(cached?.bitmap) { onStart(placeholder) }
    }

    override suspend fun success(result: SuccessResult, transition: Transition) {
        instrument(result.bitmap) { onSuccess(request, result, transition, eventListener, logger) }
    }

    override suspend fun error(result: ErrorResult, transition: Transition) {
        instrument(null) { onError(request, result, transition, eventListener, logger) }
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

    /** Increment the reference counter for the current bitmap. */
    fun increment(bitmap: Bitmap?) {
        bitmap?.let(referenceCounter::increment)
    }

    /** Replace the reference to the currently cached bitmap. */
    fun decrement(bitmap: Bitmap?) {
        target.bitmap?.let(referenceCounter::decrement)
        target.bitmap = bitmap
    }
}

private inline val RequestResult.bitmap: Bitmap?
    get() = (drawable as? BitmapDrawable)?.bitmap

private inline fun Poolable.instrument(bitmap: Bitmap?, update: PoolableViewTarget<*>.() -> Unit) {
    increment(bitmap)
    target.update()
    decrement(bitmap)
}

private suspend inline fun Target.onSuccess(
    request: Request,
    result: SuccessResult,
    transition: Transition,
    eventListener: EventListener,
    logger: Logger?
) {
    // Short circuit if this is the empty transition.
    if (transition === Transition.NONE) {
        onSuccess(result.drawable)
        return
    }

    if (this !is TransitionTarget<*>) {
        logger?.log("TargetDelegate", Log.DEBUG) {
            "Ignoring '$transition' as '$this' does not implement coil.transition.TransitionTarget."
        }
        onSuccess(result.drawable)
        return
    }

    eventListener.transitionStart(request, transition)
    transition.transition(this, result)
    eventListener.transitionEnd(request, transition)
}

private suspend inline fun Target.onError(
    request: Request,
    result: ErrorResult,
    transition: Transition,
    eventListener: EventListener,
    logger: Logger?
) {
    // Short circuit if this is the empty transition.
    if (transition === Transition.NONE) {
        onError(result.drawable)
        return
    }

    if (this !is TransitionTarget<*>) {
        logger?.log("TargetDelegate", Log.DEBUG) {
            "Ignoring '$transition' as '$this' does not implement coil.transition.TransitionTarget."
        }
        onError(result.drawable)
        return
    }

    eventListener.transitionStart(request, transition)
    transition.transition(this, result)
    eventListener.transitionEnd(request, transition)
}
