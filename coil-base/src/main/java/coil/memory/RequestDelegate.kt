package coil.memory

import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.RequestDisposable
import coil.target.ViewTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal sealed class RequestDelegate : DefaultLifecycleObserver {

    /** Called when the image request completes for any reason. */
    @MainThread
    open fun onComplete() {}

    /** Cancel any in progress work and free all resources. */
    @MainThread
    open fun onDispose() {}

    @MainThread
    override fun onDestroy(owner: LifecycleOwner) = onDispose()
}

/** A request delegate for a one-shot requests with non-poolable targets. */
internal class BaseRequestDelegate(
    private val lifecycle: Lifecycle,
    private val job: Job
) : RequestDelegate() {

    override fun onComplete() = lifecycle.removeObserver(this)

    override fun onDispose() = job.cancel()
}

/** A request delegate for a one-shot requests with poolable targets that do not implement [ViewTarget]. */
internal class PoolableTargetRequestDelegate(
    private val scope: CoroutineScope,
    private val target: TargetDelegate,
    private val lifecycle: Lifecycle,
    private val job: Job
) : RequestDelegate() {

    /** Called by [RequestDisposable.dispose] from any thread. */
    @AnyThread
    fun onDisposeUnsafe() {
        job.cancel()
        scope.launch(Dispatchers.Main.immediate) {
            target.clear()
            lifecycle.removeObserver(this@PoolableTargetRequestDelegate)
        }
    }

    override fun onDispose() {
        job.cancel()
        target.clear()
        lifecycle.removeObserver(this)
    }
}

/** A request delegate for requests with a [ViewTarget]. */
internal class ViewTargetRequestDelegate(
    private val imageLoader: ImageLoader,
    private val request: ImageRequest,
    private val target: TargetDelegate,
    private val lifecycle: Lifecycle,
    private val job: Job
) : RequestDelegate() {

    /** Repeat this request with the same params. */
    @MainThread
    fun restart() {
        imageLoader.enqueue(request)
    }

    override fun onDispose() {
        job.cancel()
        target.clear()

        if (request.target is LifecycleObserver) {
            lifecycle.removeObserver(request.target)
        }
        lifecycle.removeObserver(this)
    }
}
