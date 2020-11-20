@file:Suppress("NOTHING_TO_INLINE")

package coil.memory

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.MainThread
import coil.EventListener
import coil.ImageLoader
import coil.bitmap.BitmapReferenceCounter
import coil.bitmap.EmptyBitmapReferenceCounter
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.target.PoolableViewTarget
import coil.target.Target
import coil.transition.Transition
import coil.transition.TransitionTarget
import coil.util.Logger
import coil.util.log
import coil.util.requestManager
import coil.util.setValid

/**
 * Wrap a [Target] to support [Bitmap] pooling.
 *
 * @see DelegateService
 */
internal sealed class TargetDelegate {

    open val target: Target? get() = null

    @MainThread
    open fun start(placeholder: Drawable?, cached: Bitmap?) {}

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
 * Only invalidate the success bitmap.
 *
 * Used if [ImageRequest.target] is null and the success [Drawable] is exposed.
 *
 * @see ImageLoader.execute
 */
internal class InvalidatableEmptyTargetDelegate(
    private val referenceCounter: BitmapReferenceCounter
) : TargetDelegate() {

    override suspend fun success(result: SuccessResult) {
        referenceCounter.setValid(result.bitmap, false)
    }
}

/**
 * Invalidate the cached bitmap and the success bitmap.
 */
internal class InvalidatableTargetDelegate(
    override val target: Target,
    private val referenceCounter: BitmapReferenceCounter,
    private val eventListener: EventListener,
    private val logger: Logger?
) : TargetDelegate() {

    override fun start(placeholder: Drawable?, cached: Bitmap?) {
        referenceCounter.setValid(cached, false)
        target.onStart(placeholder)
    }

    override suspend fun success(result: SuccessResult) {
        referenceCounter.setValid(result.bitmap, false)
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
    private val referenceCounter: BitmapReferenceCounter,
    private val eventListener: EventListener,
    private val logger: Logger?
) : TargetDelegate() {

    override fun start(placeholder: Drawable?, cached: Bitmap?) {
        replace(cached) { onStart(placeholder) }
    }

    override suspend fun success(result: SuccessResult) {
        replace(result.bitmap) { onSuccess(result, eventListener, logger) }
    }

    override suspend fun error(result: ErrorResult) {
        replace(null) { onError(result, eventListener, logger) }
    }

    override fun clear() {
        replace(null) { onClear() }
    }

    /** Replace the current bitmap reference with [bitmap]. */
    private inline fun replace(bitmap: Bitmap?, update: PoolableViewTarget<*>.() -> Unit) {
        // Skip reference counting if bitmap pooling is disabled.
        if (referenceCounter is EmptyBitmapReferenceCounter) {
            target.update()
        } else {
            increment(bitmap)
            target.update()
            decrement(bitmap)
        }
    }

    /** Increment the reference counter for the current bitmap. */
    private fun increment(bitmap: Bitmap?) {
        bitmap?.let(referenceCounter::increment)
    }

    /** Replace the reference to the previous bitmap and decrement its reference count. */
    private fun decrement(bitmap: Bitmap?) {
        val previous = target.view.requestManager.put(this, bitmap)
        previous?.let(referenceCounter::decrement)
    }
}

private inline val ImageResult.bitmap: Bitmap?
    get() = (drawable as? BitmapDrawable)?.bitmap

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

    if (this !is TransitionTarget) {
        // Only log if the transition was set explicitly.
        if (result.request.defined.transition != null) {
            logger?.log(TAG, Log.DEBUG) {
                "Ignoring '$transition' as '$this' does not implement coil.transition.TransitionTarget."
            }
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

    if (this !is TransitionTarget) {
        // Only log if the transition was set explicitly.
        if (result.request.defined.transition != null) {
            logger?.log(TAG, Log.DEBUG) {
                "Ignoring '$transition' as '$this' does not implement coil.transition.TransitionTarget."
            }
        }
        onError(result.drawable)
        return
    }

    eventListener.transitionStart(result.request)
    transition.transition(this, result)
    eventListener.transitionEnd(result.request)
}

private const val TAG = "TargetDelegate"
