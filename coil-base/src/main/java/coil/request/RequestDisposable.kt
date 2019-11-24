package coil.request

import android.view.View
import coil.ImageLoader
import coil.annotation.ExperimentalCoil
import coil.target.ViewTarget
import coil.util.requestManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
     * Suspend until the current work completes.
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
 * Used for requests that are tied to a [View].
 * Requests are not disposed until a new request is attached to the view.
 */
internal class ViewTargetRequestDisposable(
    private val target: ViewTarget<*>,
    private val request: LoadRequest,
    private val scope: CoroutineScope
) : RequestDisposable {

    override val isDisposed
        get() = target.view.requestManager.currentRequest()?.request !== request

    override fun dispose() {
        // Ensure currentRequest is set from the main thread.
        scope.launch(Dispatchers.Main.immediate) {
            if (!isDisposed) {
                target.view.requestManager.setCurrentRequest(null)
            }
        }
    }

    @ExperimentalCoil
    override suspend fun await() {
        target.view.requestManager.currentRequest()?.job?.join()
    }
}
