package coil.memory

import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Job

internal sealed class RequestDelegate {

    /** Called when the image request completes for any reason. */
    @MainThread
    abstract fun onComplete()
}

/** A simple request delegate for a one-shot request. */
internal class BaseRequestDelegate(
    private val lifecycle: Lifecycle,
    private val job: Job
) : RequestDelegate(), DefaultLifecycleObserver {

    override fun onComplete() = lifecycle.removeObserver(this)

    override fun onDestroy(owner: LifecycleOwner) = job.cancel()
}

/**
 * A request delegate that has an associated view and supports restarting.
 *
 * @see ViewTargetRequestManager
 */
internal class ViewTargetRequestDelegate(
    private val imageLoader: ImageLoader,
    private val request: ImageRequest,
    private val target: TargetDelegate,
    private val lifecycle: Lifecycle,
    private val job: Job
) : RequestDelegate(), DefaultLifecycleObserver {

    /** Repeat this request with the same params. */
    @MainThread
    fun restart() {
        imageLoader.enqueue(request)
    }

    /** Cancel any in progress work and free all resources. */
    @MainThread
    fun dispose() {
        job.cancel()
        target.clear()

        if (request.target is LifecycleObserver) {
            lifecycle.removeObserver(request.target)
        }
        lifecycle.removeObserver(this)
    }

    override fun onComplete() {}

    override fun onDestroy(owner: LifecycleOwner) = dispose()
}
