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

    // The current request delegate.  This variable can temporarily diverge from
    // currentRequestJob and currentRequestJob as it must be updated from the main thread.
    private var currentRequest: ViewTargetRequestDelegate? = null

    // A pending clear operation that is posting to the main thread.
    @Volatile private var pendingClear: Job? = null

    // These fields are updated by any thread calling ImageLoader.load.
    @Volatile var currentRequestId: UUID? = null
    @Volatile var currentRequestJob: Job? = null

    private var isRestart = false
    private var skipAttach = true

    /** Return an ID to use for the next request attached to this manager. */
    @AnyThread
    fun newRequestId(): UUID {
        // Return the current request ID if this is a restarted request.
        // Restarted requests are always started from the main thread.
        val requestId = currentRequestId
        if (requestId != null && isMainThread() && isRestart) {
            return requestId
        }

        // Generate a new request ID.
        return UUID.randomUUID()
    }

    /** Replace the current request attached to this view. */
    @MainThread
    fun setCurrentRequest(request: ViewTargetRequestDelegate?) {
        // Don't cancel the pending clear if this is a restarted request.
        if (isRestart) {
            isRestart = false
        } else {
            pendingClear?.cancel()
            pendingClear = null
        }

        currentRequest?.dispose()
        currentRequest = request
        skipAttach = true
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
        } else {
            currentRequest?.let { request ->
                isRestart = true
                request.restart()
            }
        }
    }

    @MainThread
    override fun onViewDetachedFromWindow(v: View) {
        skipAttach = false
        currentRequest?.dispose()
    }
}
