package coil3.request

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import coil3.ImageLoader
import coil3.annotation.MainThread
import coil3.target.ViewTarget
import coil3.util.awaitStarted
import coil3.util.removeAndAddObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

/** A request delegate for a one-shot requests with no target or a non-[ViewTarget]. */
internal class LifecycleRequestDelegate(
    private val lifecycle: Lifecycle,
    private val job: Job,
) : RequestDelegate, DefaultLifecycleObserver {

    override fun start() {
        lifecycle.addObserver(this)
    }

    override suspend fun awaitStarted() {
        lifecycle.awaitStarted()
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
    private val lifecycle: Lifecycle?,
    private val job: Job,
) : RequestDelegate, DefaultLifecycleObserver {

    /** Repeat this request with the same [ImageRequest]. */
    @MainThread
    fun restart() {
        imageLoader.enqueue(initialRequest)
    }

    override fun start() {
        // Cancel the request before starting if the view is not attached.
        // It will be restarted automatically when the view is attached.
        if (!target.view.isAttachedToWindow) {
            target.view.requestManager.setRequest(this)
            throw CancellationException("'ViewTarget.view' must be attached to a window.")
        }

        lifecycle?.addObserver(this)
        if (target is LifecycleObserver) {
            lifecycle?.removeAndAddObserver(target)
        }
        target.view.requestManager.setRequest(this)
    }

    override suspend fun awaitStarted() {
        lifecycle?.awaitStarted()
    }

    override fun dispose() {
        job.cancel()
        if (target is LifecycleObserver) {
            lifecycle?.removeObserver(target)
        }
        lifecycle?.removeObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        target.view.requestManager.dispose()
    }
}
