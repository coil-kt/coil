package coil.request

import android.view.View
import coil.ImageLoader
import coil.annotation.ExperimentalCoil
import coil.target.ViewTarget
import coil.util.requestManager
import kotlinx.coroutines.Job

/**
 * Represents the work of an [ImageLoader.load] request.
 */
interface RequestDisposable {

    /**
     * Return true if the request is complete or cancelling.
     */
    val isDisposed: Boolean

    /**
     * Cancel any in progress work and free any resources associated with this request. This method is idempotent.
     */
    fun dispose()

    /**
     * Suspend until any in progress work completes.
     */
    @ExperimentalCoil
    suspend fun await()
}

/** Used for a one-shot image request. */
internal class BaseTargetRequestDisposable(private val job: Job) : RequestDisposable {

    override val isDisposed
        get() = !job.isActive

    override fun dispose() {
        if (!isDisposed) {
            job.cancel()
        }
    }

    @ExperimentalCoil
    override suspend fun await() = job.join()
}

/**
 * Used for requests that are attached to a [View].
 *
 * As requests attached to a view can be restarted when the view is re-attached, [isDisposed] will
 * return true until [dispose] is called or another request has been set on the view.
 */
internal class ViewTargetRequestDisposable(
    private val target: ViewTarget<*>,
    private val request: LoadRequest
) : RequestDisposable {

    override var isDisposed = false
        get() = field || target.view.requestManager.currentRequest?.request !== request
        private set

    override fun dispose() {
        if (!isDisposed) {
            isDisposed = true
            target.view.requestManager.clearCurrentRequest()
        }
    }

    @ExperimentalCoil
    override suspend fun await() {
        if (!isDisposed) {
            target.view.requestManager.currentRequest?.job?.join()
        }
    }
}
