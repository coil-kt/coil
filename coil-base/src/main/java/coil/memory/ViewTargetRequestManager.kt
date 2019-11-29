package coil.memory

import android.view.View
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import coil.request.Request
import coil.util.requestManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Ensures that at most one [Request] can be attached to a given [View] at one time.
 *
 * @see requestManager
 */
internal class ViewTargetRequestManager : View.OnAttachStateChangeListener {

    @Volatile private var pendingClear: Job? = null
    @Volatile private var currentRequest: ViewTargetRequestDelegate? = null

    private var isRestart = false
    private var skipAttach = true

    /** Get the current request attached to this view. */
    fun getCurrentRequest() = currentRequest

    /** Replace the current request attached to this view. */
    @MainThread
    fun setCurrentRequest(request: ViewTargetRequestDelegate?) {
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
        pendingClear?.cancel()
        pendingClear = CoroutineScope(Dispatchers.Main.immediate).launch { setCurrentRequest(null) }
    }

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

    override fun onViewDetachedFromWindow(v: View) {
        skipAttach = false
        currentRequest?.dispose()
    }
}
