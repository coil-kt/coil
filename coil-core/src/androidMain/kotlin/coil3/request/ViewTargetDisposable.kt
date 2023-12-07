package coil3.request

import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import coil3.target.ViewTarget
import kotlinx.coroutines.Deferred

/**
 * A disposable for requests that are attached to a [View].
 *
 * [ViewTarget] requests are automatically cancelled in when the view is detached
 * and are restarted when the view is attached.
 *
 * [isDisposed] only returns 'true' when this disposable's request is cleared (due to
 * [DefaultLifecycleObserver.onDestroy]) or replaced by a new request attached to the view.
 */
internal class ViewTargetDisposable(
    private val view: View,
    @Volatile override var job: Deferred<ImageResult>,
) : Disposable {

    override val isDisposed: Boolean
        get() = view.requestManager.isDisposed(this)

    override fun dispose() {
        if (isDisposed) return
        view.requestManager.dispose()
    }
}
