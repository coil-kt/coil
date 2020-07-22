@file:OptIn(ExperimentalCoilApi::class)
@file:Suppress("NOTHING_TO_INLINE")

package coil.memory

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.MainThread
import coil.EventListener
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.bitmap.BitmapReferenceCounter
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.RequestResult
import coil.request.SuccessResult
import coil.target.PoolableViewTarget
import coil.target.Target
import coil.transition.Transition
import coil.transition.TransitionTarget
import coil.util.Logger
import coil.util.log
import coil.util.requestManager

/**
 * Wrap a [Target] to support [Bitmap] pooling.
 *
 * @see DelegateService
 */
internal sealed class TargetDelegate {

    open val target: Target? get() = null

    @MainThread
    open fun start(cached: BitmapDrawable?, placeholder: Drawable?) {}

    @MainThread
    open suspend fun success(result: SuccessResult) {}

    @MainThread
    open suspend fun error(result: ErrorResult) {}

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
 * Used if [ImageRequest.target] is null and the success [Drawable] is leaked.
 *
 * @see ImageLoader.execute
 */
internal class InvalidatableEmptyTargetDelegate(
    override val referenceCounter: BitmapReferenceCounter
) : TargetDelegate(), Invalidatable {

    override suspend fun success(result: SuccessResult) {
        invalidate(result.bitmap)
    }
}

/**
 * Invalidate the cached bitmap and the success bitmap.
 */
internal class InvalidatableTargetDelegate(
    override val target: Target,
    override val referenceCounter: BitmapReferenceCounter,
    private val eventListener: EventListener,
    private val logger: Logger?
) : TargetDelegate(), Invalidatable {

    override fun start(cached: BitmapDrawable?, placeholder: Drawable?) {
        invalidate(cached?.bitmap)
        target.onStart(placeholder)
    }

    override suspend fun success(result: SuccessResult) {
        invalidate(result.bitmap)
        target.onSuccess(result, eventListener, logger)
    }

    override suspend fun error(result: ErrorResult) {
        target.onError(result, eventListener, logger)
    }
}

/**
 * Handle the reference counts for the cached bitmap and the success bitmap.
 */
internal class PoolableTargetDelegate(
    override val target: PoolableViewTarget<*>,
    override val referenceCounter: BitmapReferenceCounter,
    private val eventListener: EventListener,
    private val logger: Logger?
) : TargetDelegate(), Poolable {

    override fun start(cached: BitmapDrawable?, placeholder: Drawable?) {
        instrument(cached?.bitmap) { onStart(placeholder) }
    }

    override suspend fun success(result: SuccessResult) {
        instrument(result.bitmap) { onSuccess(result, eventListener, logger) }
    }

    override suspend fun error(result: ErrorResult) {
        instrument(null) { onError(result, eventListener, logger) }
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

    val target: PoolableViewTarget<*>
    val referenceCounter: BitmapReferenceCounter

    /** Increment the reference counter for the current bitmap. */
    fun increment(bitmap: Bitmap?) {
        bitmap?.let(referenceCounter::increment)
    }

    /** Replace the reference to the currently cached bitmap. */
    fun decrement(bitmap: Bitmap?) {
        val previous = target.view.requestManager.put(this, bitmap)
        previous?.let(referenceCounter::decrement)
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
    result: SuccessResult,
    eventListener: EventListener,
    logger: Logger?
) {
    // Short circuit if this is the empty transition.
    val transition = result.request.transition
    if (transition === Transition.NONE) {
        onSuccess(result.drawable)
        return
    }

    if (this !is TransitionTarget<*>) {
        logger?.log(TAG, Log.DEBUG) {
            "Ignoring '$transition' as '$this' does not implement coil.transition.TransitionTarget."
        }
        onSuccess(result.drawable)
        return
    }

    eventListener.transitionStart(result.request)
    transition.transition(this, result)
    eventListener.transitionEnd(result.request)
}

private suspend inline fun Target.onError(
    result: ErrorResult,
    eventListener: EventListener,
    logger: Logger?
) {
    // Short circuit if this is the empty transition.
    val transition = result.request.transition
    if (transition === Transition.NONE) {
        onError(result.drawable)
        return
    }

    if (this !is TransitionTarget<*>) {
        logger?.log(TAG, Log.DEBUG) {
            "Ignoring '$transition' as '$this' does not implement coil.transition.TransitionTarget."
        }
        onError(result.drawable)
        return
    }

    eventListener.transitionStart(result.request)
    transition.transition(this, result)
    eventListener.transitionEnd(result.request)
}

private const val TAG = "TargetDelegate"
