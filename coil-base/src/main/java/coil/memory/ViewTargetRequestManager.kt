package coil.memory

import android.view.View
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

    private var clearCurrentRequestJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }
    var currentRequest: ViewTargetRequestDelegate? = null
        private set

    private var isRestart = false
    private var skipAttach = true

    @MainThread
    fun setCurrentRequest(request: ViewTargetRequestDelegate?) {
        if (isRestart) {
            isRestart = false
        } else {
            clearCurrentRequestJob = null
        }

        currentRequest?.dispose()
        currentRequest = request
        skipAttach = true
    }

    /** Detach the current request from this view. */
    fun clearCurrentRequest() {
        clearCurrentRequestJob = CoroutineScope(Job() + Dispatchers.Main.immediate).launch {
            setCurrentRequest(null)
        }
    }

    override fun onViewAttachedToWindow(v: View) {
        if (skipAttach) {
            skipAttach = false
        } else {
            isRestart = true
            currentRequest?.restart()
        }
    }

    override fun onViewDetachedFromWindow(v: View) {
        skipAttach = false
        currentRequest?.dispose()
    }
}
