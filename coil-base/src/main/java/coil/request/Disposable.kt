package coil.request

import android.view.View
import coil.annotation.ExperimentalCoilApi
import coil.target.ViewTarget
import coil.util.requestManager
import kotlinx.coroutines.Job
import java.util.UUID

/**
 * Represents the work of an executed [ImageRequest].
 */
interface Disposable {

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
 * A disposable for one-shot image requests.
 */
internal class BaseTargetDisposable(private val job: Job) : Disposable {

    override val isDisposed
        get() = !job.isActive

    override fun dispose() {
        if (isDisposed) return
        job.cancel()
    }

    @ExperimentalCoilApi
    override suspend fun await() {
        if (isDisposed) return
        job.join()
    }
}

/**
 * A disposable for requests that are attached to a [View].
 *
 * [ViewTargetDisposable] is not disposed until its request is detached from the view.
 * This is because requests are automatically cancelled in [View.onDetachedFromWindow] and are
 * restarted in [View.onAttachedToWindow].
 */
internal class ViewTargetDisposable(
    private val requestId: UUID,
    private val target: ViewTarget<*>
) : Disposable {

    override val isDisposed
        get() = target.view.requestManager.currentRequestId != requestId

    override fun dispose() {
        if (isDisposed) return
        target.view.requestManager.clearCurrentRequest()
    }

    @ExperimentalCoilApi
    override suspend fun await() {
        if (isDisposed) return
        target.view.requestManager.currentRequestJob?.join()
    }
}
