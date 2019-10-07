package coil.memory

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
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
        val target = request.target
        return when {
            request is GetRequest -> InvalidatableEmptyTargetDelegate(referenceCounter)
            target == null -> EmptyTargetDelegate
            target is PoolableViewTarget<*> -> PoolableTargetDelegate(target, referenceCounter)
            else -> InvalidatableTargetDelegate(target, referenceCounter)
        }
    }

    /** Wrap [request] to automatically dispose (and for [ViewTarget]s restart) the [Request] based on its lifecycle. */
    fun createRequestDelegate(
        request: Request,
        targetDelegate: TargetDelegate,
        lifecycle: Lifecycle,
        mainDispatcher: CoroutineDispatcher,
        deferred: Deferred<Drawable>
    ): RequestDelegate {
        val requestDelegate = when (request) {
            is GetRequest -> EmptyRequestDelegate
            is LoadRequest -> if (request.target is ViewTarget<*>) {
                ViewTargetRequestDelegate(imageLoader, request, targetDelegate, lifecycle, mainDispatcher, deferred)
            } else {
                BaseRequestDelegate(lifecycle, mainDispatcher, deferred)
            }
        }

        lifecycle.addObserver(requestDelegate)

        val target = request.target
        if (target is ViewTarget<*>) {
            target.requestManager.setRequest(requestDelegate)
        }

        return requestDelegate
    }
}
