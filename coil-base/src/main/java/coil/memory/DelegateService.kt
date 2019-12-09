package coil.memory

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import coil.ImageLoader
import coil.request.GetRequest
import coil.request.LoadRequest
import coil.request.Request
import coil.target.PoolableViewTarget
import coil.target.Target
import coil.target.ViewTarget
import coil.util.requestManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred

/**
 * [DelegateService] wraps [Target]s to support [Bitmap] pooling and [Request]s to manage their lifecycle.
 */
internal class DelegateService(
    private val imageLoader: ImageLoader,
    private val referenceCounter: BitmapReferenceCounter
) {

    /** Wrap the [request]'s [Target] to support [Bitmap] pooling. */
    fun createTargetDelegate(request: Request): TargetDelegate {
        return when (request) {
            is GetRequest -> InvalidatableEmptyTargetDelegate(referenceCounter)
            is LoadRequest -> when (val target = request.target) {
                null -> EmptyTargetDelegate
                is PoolableViewTarget<*> -> PoolableTargetDelegate(target, referenceCounter)
                else -> InvalidatableTargetDelegate(target, referenceCounter)
            }
        }
    }

    /** Wrap [request] to automatically dispose (and for [ViewTarget]s restart) the [Request] based on its lifecycle. */
    @MainThread
    fun createRequestDelegate(
        request: Request,
        targetDelegate: TargetDelegate,
        lifecycle: Lifecycle,
        mainDispatcher: CoroutineDispatcher,
        deferred: Deferred<Drawable>
    ): RequestDelegate {
        val requestDelegate: RequestDelegate

        when (request) {
            is GetRequest -> {
                requestDelegate = EmptyRequestDelegate
                lifecycle.addObserver(requestDelegate)
            }
            is LoadRequest -> when (val target = request.target) {
                is ViewTarget<*> -> {
                    requestDelegate = ViewTargetRequestDelegate(
                        loader = imageLoader,
                        request = request,
                        target = targetDelegate,
                        lifecycle = lifecycle,
                        dispatcher = mainDispatcher,
                        job = deferred
                    )
                    lifecycle.addObserver(requestDelegate)

                    // Attach this request to the target's view.
                    target.view.requestManager.setCurrentRequest(requestDelegate)
                }
                else -> {
                    requestDelegate = BaseRequestDelegate(lifecycle, mainDispatcher, deferred)
                    lifecycle.addObserver(requestDelegate)
                }
            }
        }

        return requestDelegate
    }
}
