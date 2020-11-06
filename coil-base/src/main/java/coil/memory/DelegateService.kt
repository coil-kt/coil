package coil.memory

import android.graphics.Bitmap
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleObserver
import coil.EventListener
import coil.ImageLoader
import coil.bitmap.BitmapReferenceCounter
import coil.request.ImageRequest
import coil.target.PoolableViewTarget
import coil.target.Target
import coil.target.ViewTarget
import coil.util.Logger
import coil.util.Utils.REQUEST_TYPE_ENQUEUE
import coil.util.Utils.REQUEST_TYPE_EXECUTE
import coil.util.isAttachedToWindowCompat
import coil.util.requestManager
import kotlinx.coroutines.Job

/** [DelegateService] wraps [Target]s to support [Bitmap] pooling and [ImageRequest]s to manage their lifecycle. */
internal class DelegateService(
    private val imageLoader: ImageLoader,
    private val referenceCounter: BitmapReferenceCounter,
    private val logger: Logger?
) {

    /** Wrap the [Target] to support [Bitmap] pooling. */
    @MainThread
    fun createTargetDelegate(
        target: Target?,
        type: Int,
        eventListener: EventListener
    ): TargetDelegate {
        return when (type) {
            REQUEST_TYPE_EXECUTE -> when (target) {
                null -> InvalidatableEmptyTargetDelegate(referenceCounter)
                else -> InvalidatableTargetDelegate(target, referenceCounter, eventListener, logger)
            }
            REQUEST_TYPE_ENQUEUE -> when (target) {
                null -> EmptyTargetDelegate
                is PoolableViewTarget<*> -> PoolableTargetDelegate(target, referenceCounter, eventListener, logger)
                else -> InvalidatableTargetDelegate(target, referenceCounter, eventListener, logger)
            }
            else -> error("Invalid type.")
        }
    }

    /** Wrap [request] to automatically dispose (and for [ViewTarget]s restart) the [ImageRequest] based on its lifecycle. */
    @MainThread
    fun createRequestDelegate(
        request: ImageRequest,
        targetDelegate: TargetDelegate,
        job: Job
    ): RequestDelegate {
        val lifecycle = request.lifecycle
        val delegate: RequestDelegate
        when (val target = request.target) {
            is ViewTarget<*> -> {
                delegate = ViewTargetRequestDelegate(imageLoader, request, targetDelegate, job)
                lifecycle.addObserver(delegate)

                if (target is LifecycleObserver) {
                    lifecycle.removeObserver(target)
                    lifecycle.addObserver(target)
                }

                target.view.requestManager.setCurrentRequest(delegate)

                // Call onViewDetachedFromWindow immediately if the view is already detached.
                if (!target.view.isAttachedToWindowCompat) {
                    target.view.requestManager.onViewDetachedFromWindow(target.view)
                }
            }
            else -> {
                delegate = BaseRequestDelegate(lifecycle, job)
                lifecycle.addObserver(delegate)
            }
        }
        return delegate
    }
}
