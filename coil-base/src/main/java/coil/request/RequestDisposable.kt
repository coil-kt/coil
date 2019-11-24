package coil.request

import androidx.annotation.MainThread
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
     * Return true if the request is completed or cancelling.
     */
    val isDisposed: Boolean

    /**
     * Cancel any in progress work and free any resources associated with this request. This method is idempotent.
     */
    @MainThread
    fun dispose()

    /**
     * Suspend until the current request completes or is cancelled.
     */
    @ExperimentalCoil
    suspend fun await()
}

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

internal class ViewTargetRequestDisposable(
    private val target: ViewTarget<*>,
    private val request: LoadRequest,
    private val job: Job
) : RequestDisposable {

    override val isDisposed
        get() = target.view.requestManager.currentRequest?.request != request

    override fun dispose() {
        if (!isDisposed) {
            target.view.requestManager.currentRequest = null
        }
    }

    @ExperimentalCoil
    override suspend fun await() = job.join()
}
