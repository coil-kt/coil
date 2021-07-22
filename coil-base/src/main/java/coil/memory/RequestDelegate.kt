package coil.memory

import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import coil.request.ImageRequest
import coil.target.ViewTarget
import coil.util.removeAndAddObserver
import coil.util.requestManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

internal sealed class RequestDelegate : DefaultLifecycleObserver {

    /** Throw a [CancellationException] if this request should be cancelled before starting. */
    @MainThread
    open fun assertActive() {}

    /** Register all lifecycle observers. */
    @MainThread
    open fun start() {}

    /** Called when this request's job is cancelled or completes successfully/unsuccessfully. */
    @MainThread
    open fun complete() {}

    /** Cancel this request's job and clear all lifecycle observers. */
    @MainThread
    open fun dispose() {}
}

/** A request delegate for a one-shot requests with no target or a non-[ViewTarget]. */
internal class BaseRequestDelegate(
    private val lifecycle: Lifecycle,
    private val job: Job
) : RequestDelegate() {

    override fun start() {
        lifecycle.addObserver(this)
    }

    override fun complete() {
        lifecycle.removeObserver(this)
    }

    override fun dispose() {
        job.cancel()
    }

    override fun onDestroy(owner: LifecycleOwner) = dispose()
}

/** A request delegate for restartable requests with a [ViewTarget]. */
internal class ViewTargetRequestDelegate(
    private val imageLoader: ImageLoader,
    private val initialRequest: ImageRequest,
    private val target: ViewTarget<*>,
    private val lifecycle: Lifecycle,
    private val job: Job
) : RequestDelegate() {

    /** Repeat this request with the same [ImageRequest]. */
    @MainThread
    fun restart() {
        imageLoader.enqueue(initialRequest)
    }

    override fun assertActive() {
        if (!target.view.isAttachedToWindow) {
            target.view.requestManager.setRequest(this)
            throw CancellationException("'ViewTarget.view' must be attached to a window.")
        }
    }

    override fun start() {
        lifecycle.addObserver(this)
        if (target is LifecycleObserver) lifecycle.removeAndAddObserver(target)
        target.view.requestManager.setRequest(this)
    }

    override fun dispose() {
        job.cancel()
        if (target is LifecycleObserver) lifecycle.removeObserver(target)
        lifecycle.removeObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        target.view.requestManager.dispose()
    }
}
