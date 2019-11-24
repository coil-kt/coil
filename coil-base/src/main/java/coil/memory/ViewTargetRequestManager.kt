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

    private var skipAttach = true

    var currentRequest: ViewTargetRequestDelegate? = null
        @MainThread set(value) {
            field?.dispose()
            field = value
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
