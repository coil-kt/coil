package coil.memory

import android.view.View

internal class ViewTargetRequestManager : View.OnAttachStateChangeListener {

    private var request: RequestDelegate? = null
    private var skipAttach = true

    fun getRequest() = request

    fun setRequest(request: RequestDelegate?) {
        this.request?.dispose()
        this.request = request
        this.skipAttach = true
    }

    override fun onViewAttachedToWindow(v: View) {
        if (skipAttach) {
            skipAttach = false
        } else {
            request?.restart()
        }
    }

    override fun onViewDetachedFromWindow(v: View) {
        skipAttach = false
        request?.dispose()
    }
}
