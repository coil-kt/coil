package coil.memory

import android.view.View
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import coil.request.Request
import coil.util.isMainThread
import coil.util.requestManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Ensures that at most one [Request] can be attached to a given [View] at one time.
 *
 * @see requestManager
 */
internal class ViewTargetRequestManager : View.OnAttachStateChangeListener {

    // Only accessed from the main thread. The request delegate for the most recently dispatched request.
    private var currentRequest: ViewTargetRequestDelegate? = null

    // Metadata about the current request (that may have not been dispatched yet).
    @Volatile var currentRequestId: UUID? = null
        private set
    @Volatile var currentRequestJob: Job? = null
        private set

    // A pending operation that is posting to the main thread to clear the current request.
    @Volatile private var pendingClear: Job? = null

    // Only accessed from the main thread.
    private var isRestart = false
    private var skipAttach = true

    /** Attach [request] to this view and dispose the old request. */
    @MainThread
    fun setCurrentRequest(request: ViewTargetRequestDelegate?) {
        // Don't cancel the pending clear if this is a restarted request.
        if (isRestart) {
            isRestart = false
        } else {
            pendingClear?.cancel()
            pendingClear = null
        }

        currentRequest?.cancel()
        currentRequest = request
        skipAttach = true
    }

    /** Set the current [job] attached to this view and assign it an ID. */
    @AnyThread
    fun setCurrentRequestJob(job: Job): UUID {
        val requestId = newRequestId()
        currentRequestId = requestId
        currentRequestJob = job
        return requestId
    }

    /** Detach the current request from this view. */
    @AnyThread
    fun clearCurrentRequest() {
        currentRequestId = null
        currentRequestJob = null

        pendingClear?.cancel()
        pendingClear = CoroutineScope(Dispatchers.Main.immediate).launch { setCurrentRequest(null) }
    }

    @MainThread
    override fun onViewAttachedToWindow(v: View) {
        if (skipAttach) {
            skipAttach = false
            return
        }

        currentRequest?.let { request ->
            // As this is called from the main thread, isRestart will
            // be cleared synchronously as part of request.restart().
            isRestart = true
            request.restart()
        }
    }

    @MainThread
    override fun onViewDetachedFromWindow(v: View) {
        skipAttach = false
        currentRequest?.cancel()
    }

    /** Return an ID to use for the next request attached to this manager. */
    @AnyThread
    private fun newRequestId(): UUID {
        // Return the current request ID if this is a restarted request.
        // Restarted requests are always launched from the main thread.
        val requestId = currentRequestId
        if (requestId != null && isRestart && isMainThread()) {
            return requestId
        }

        // Generate a new request ID.
        return UUID.randomUUID()
    }
}
