@file:Suppress("NOTHING_TO_INLINE")

package coil.memory

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.MainThread
import coil.EventListener
import coil.request.ErrorResult
import coil.request.SuccessResult
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
 * Invalidate the cached bitmap and the success bitmap.
 */
internal class InvalidatableTargetDelegate(
    override val target: Target,
    private val eventListener: EventListener,
    private val logger: Logger?
) : TargetDelegate() {

    override fun start(placeholder: Drawable?, cached: Bitmap?) {
        target.onStart(placeholder)
    }

    override suspend fun success(result: SuccessResult) {
        target.onSuccess(result, eventListener, logger)
    }

    override suspend fun error(result: ErrorResult) {
        target.onError(result, eventListener, logger)
    }
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
