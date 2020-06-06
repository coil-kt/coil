package coil.memory

import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import coil.request.Request
import kotlinx.coroutines.Job

internal sealed class RequestDelegate : DefaultLifecycleObserver {

    /** The underlying job for this request. */
    abstract val job: Job

    /** Called when the image request completes for any reason. */
    @MainThread
    abstract fun complete()

    @MainThread
    override fun onDestroy(owner: LifecycleOwner) = job.cancel()
}

/** A simple request delegate for a one-shot request. */
internal class BaseRequestDelegate(
    private val lifecycle: Lifecycle,
    override val job: Job
): RequestDelegate() {

    override fun complete() {
        lifecycle.removeObserver(this)
    }
}

/**
 * A request delegate that has an associated view and supports restarting.
 *
 * @see ViewTargetRequestManager
 */
internal class ViewTargetRequestDelegate(
    private val imageLoader: ImageLoader,
    private val request: Request,
    private val target: TargetDelegate,
    private val lifecycle: Lifecycle,
    override val job: Job
) : RequestDelegate() {

    /** Cancel the request if it is in progress. */
    fun cancel() {
        job.cancel()
    }

    /** Repeat this request with the same params. */
    @MainThread
    fun restart() {
        imageLoader.enqueue(request)
    }

    override fun complete() {
        target.clear()
        if (request.target is LifecycleObserver) {
            lifecycle.removeObserver(request.target)
        }
        lifecycle.removeObserver(this)
    }
}
