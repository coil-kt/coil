package coil.memory

import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import coil.request.LoadRequest
import kotlinx.coroutines.Job

internal sealed class RequestDelegate {

    /** Cancel any in progress work and free any resources associated with this delegate. */
    @MainThread
    open fun dispose() {}

    /** Called when the image request completes for any reason. */
    @MainThread
    open fun onComplete() {}
}

/** An empty request delegate. */
internal object EmptyRequestDelegate : RequestDelegate()

/** A simple request delegate for a one-shot request. */
internal class BaseRequestDelegate(
    private val lifecycle: Lifecycle,
    private val job: Job
) : RequestDelegate(), DefaultLifecycleObserver {

    override fun dispose() = job.cancel()

    override fun onComplete() = lifecycle.removeObserver(this)

    override fun onDestroy(owner: LifecycleOwner) = dispose()
}

/**
 * A request delegate that has an associated view and supports restarting.
 *
 * @see ViewTargetRequestManager
 */
internal class ViewTargetRequestDelegate(
    private val imageLoader: ImageLoader,
    private val request: LoadRequest,
    private val target: TargetDelegate,
    private val lifecycle: Lifecycle,
    private val job: Job
) : RequestDelegate(), DefaultLifecycleObserver {

    /** Repeat this request with the same params. */
    @MainThread
    fun restart() {
        imageLoader.execute(request)
    }

    override fun dispose() {
        job.cancel()
        target.clear()

        if (request.target is LifecycleObserver) {
            lifecycle.removeObserver(request.target)
        }
        lifecycle.removeObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) = dispose()
}
