package coil.request

import coil.memory.ViewTargetRequestDelegate
import coil.target.ViewTarget
import coil.util.cancel
import coil.util.requestManager
import kotlinx.coroutines.Job

/**
 * Represents the work of an image request.
 */
interface RequestDisposable {

    /**
     * Return true if request is not active, completed, or cancelling.
     */
    fun isDisposed(): Boolean

    /**
     * Cancel any in progress work and free any resources associated with this request. This method is idempotent.
     */
    fun dispose()
}

internal object EmptyRequestDisposable : RequestDisposable {

    override fun isDisposed() = true

    override fun dispose() {}
}

internal class BaseTargetRequestDisposable(private val job: Job) : RequestDisposable {

    override fun isDisposed(): Boolean {
        return !job.isActive || job.isCompleted
    }

    override fun dispose() {
        if (!isDisposed()) {
            job.cancel()
        }
    }
}

internal class ViewTargetRequestDisposable(
    private val target: ViewTarget<*>,
    private val request: Request
) : RequestDisposable {

    /**
     * Check if the current request attached to this view is the same as this disposable's request.
     */
    override fun isDisposed(): Boolean {
        return (target.requestManager.getRequest() as? ViewTargetRequestDelegate)?.request != request
    }

    override fun dispose() {
        if (!isDisposed()) {
            target.cancel()
        }
    }
}
