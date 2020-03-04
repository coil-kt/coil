package coil.request

import android.view.View
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.target.ViewTarget
import coil.util.requestManager
import kotlinx.coroutines.Job
import java.util.UUID

/**
 * Represents the work of an [ImageLoader.load] request.
 */
interface RequestDisposable {

    /**
     * Returns true if the request is complete or cancelling.
     */
    val isDisposed: Boolean

    /**
     * Cancels any in progress work and frees any resources associated with this request. This method is idempotent.
     */
    fun dispose()

    /**
     * Suspends until any in progress work completes.
     */
    @ExperimentalCoilApi
    suspend fun await()
}

/**
 * Used for one-shot image requests.
 */
internal class BaseTargetRequestDisposable(private val job: Job) : RequestDisposable {

    override val isDisposed
        get() = !job.isActive

    override fun dispose() {
        if (!isDisposed) {
            job.cancel()
        }
    }

    @ExperimentalCoilApi
    override suspend fun await() {
        if (!isDisposed) {
            job.join()
        }
    }
}

/**
 * Used for requests that are attached to a [View].
 *
 * [ViewTargetRequestDisposable] is not disposed until its request is detached from the view.
 * This is because requests are automatically cancelled in [View.onDetachedFromWindow]
 * and are restarted in [View.onAttachedToWindow].
 */
internal class ViewTargetRequestDisposable(
    private val requestId: UUID,
    private val target: ViewTarget<*>
) : RequestDisposable {

    override val isDisposed
        get() = target.view.requestManager.currentRequestId != requestId

    override fun dispose() {
        if (!isDisposed) {
            target.view.requestManager.clearCurrentRequest()
        }
    }

    @ExperimentalCoilApi
    override suspend fun await() {
        if (!isDisposed) {
            target.view.requestManager.currentRequestJob?.join()
        }
    }
}
