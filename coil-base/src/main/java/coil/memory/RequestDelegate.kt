package coil.memory

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import coil.request.LoadRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job

internal sealed class RequestDelegate : DefaultLifecycleObserver {

    /**
     * Repeat this request with the same params.
     */
    open fun restart() {}

    /**
     * Free resources associated with this delegate.
     */
    open fun dispose() {}

    /**
     * Called when the load completes.
     */
    open fun onComplete() {}
}

/**
 * An empty request delegate.
 */
internal object EmptyRequestDelegate : RequestDelegate()

/**
 * A simple request delegate that does not support restarting.
 */
internal class BaseRequestDelegate(
    private val lifecycle: Lifecycle,
    private val dispatcher: CoroutineDispatcher,
    private val job: Job
) : RequestDelegate() {

    override fun dispose() = job.cancel()

    override fun onComplete() {
        if (dispatcher is LifecycleObserver) {
            lifecycle.removeObserver(dispatcher)
        }
        lifecycle.removeObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) = dispose()
}

/**
 * A request delegate that has an associated view and supports restarting.
 *
 * @see ViewTargetRequestManager
 */
internal class ViewTargetRequestDelegate(
    private val loader: ImageLoader,
    internal val request: LoadRequest,
    private val target: TargetDelegate,
    private val lifecycle: Lifecycle,
    private val dispatcher: CoroutineDispatcher,
    private val job: Job
) : RequestDelegate() {

    override fun restart() {
        loader.load(request)
    }

    override fun dispose() {
        job.cancel()
        target.clear()

        if (request.target is LifecycleObserver) {
            lifecycle.removeObserver(request.target)
        }
        lifecycle.removeObserver(this)
    }

    override fun onComplete() {
        if (dispatcher is LifecycleObserver) {
            lifecycle.removeObserver(dispatcher)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) = dispose()
}
