package coil.memory

import android.graphics.Bitmap
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import coil.EventListener
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.request.Request
import coil.target.PoolableTarget
import coil.target.Target
import coil.target.ViewTarget
import coil.util.Logger
import coil.util.Utils.REQUEST_TYPE_ENQUEUE
import coil.util.Utils.REQUEST_TYPE_EXECUTE
import coil.util.requestManager
import kotlinx.coroutines.Job

/** [DelegateService] wraps [Target]s to support [Bitmap] pooling and [Request]s to manage their lifecycle. */
@OptIn(ExperimentalCoilApi::class)
internal class DelegateService(
    private val imageLoader: ImageLoader,
    private val referenceCounter: BitmapReferenceCounter,
    private val logger: Logger?
) {

    /** Wrap the [request]'s [Target] to support [Bitmap] pooling. */
    fun createTargetDelegate(
        request: Request,
        type: Int,
        eventListener: EventListener
    ): TargetDelegate {
        return when (type) {
            REQUEST_TYPE_EXECUTE -> when (val target = request.target) {
                null -> InvalidatableEmptyTargetDelegate(referenceCounter)
                else -> InvalidatableTargetDelegate(request, target, referenceCounter, eventListener, logger)
            }
            REQUEST_TYPE_ENQUEUE -> when (val target = request.target) {
                null -> EmptyTargetDelegate
                is PoolableTarget -> PoolableTargetDelegate(request, target, referenceCounter, eventListener, logger)
                else -> InvalidatableTargetDelegate(request, target, referenceCounter, eventListener, logger)
            }
            else -> error("Invalid type.")
        }
    }

    /** Wrap [request] to automatically dispose (and for [ViewTarget]s restart) the [Request] based on its lifecycle. */
    @MainThread
    fun createRequestDelegate(
        job: Job,
        targetDelegate: TargetDelegate,
        request: Request,
        lifecycle: Lifecycle
    ): RequestDelegate {
        val delegate: RequestDelegate
        if (request.target is ViewTarget<*>) {
            delegate = ViewTargetRequestDelegate(imageLoader, request, targetDelegate, lifecycle, job)
            lifecycle.addObserver(delegate)
            request.target.view.requestManager.setCurrentRequest(delegate)
        } else {
            delegate = BaseRequestDelegate(lifecycle, job)
            lifecycle.addObserver(delegate)
        }
        return delegate
    }
}
