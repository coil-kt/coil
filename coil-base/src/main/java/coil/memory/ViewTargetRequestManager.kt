package coil.memory

import android.view.View
import androidx.annotation.MainThread
import coil.request.Request
import coil.util.requestManager

/**
 * Ensures that at most one [Request] can be attached to a given [View] at one time.
 *
 * @see requestManager
 */
internal class ViewTargetRequestManager : View.OnAttachStateChangeListener {

    private var currentRequest: ViewTargetRequestDelegate? = null
    private var skipAttach = true

    fun currentRequest() = currentRequest

    @MainThread
    fun setCurrentRequest(request: ViewTargetRequestDelegate?) {
        currentRequest?.dispose()
        currentRequest = request
        skipAttach = true
    }

    override fun onViewAttachedToWindow(v: View) {
        if (skipAttach) {
            skipAttach = false
        } else {
            currentRequest?.restart()
        }
    }

    override fun onViewDetachedFromWindow(v: View) {
        skipAttach = false
        currentRequest?.dispose()
    }
}
