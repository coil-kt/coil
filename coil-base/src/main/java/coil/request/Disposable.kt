package coil.request

import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import coil.ImageLoader
import coil.target.ViewTarget
import coil.util.requestManager
import kotlinx.coroutines.Deferred

/**
 * Represents the work of an [ImageRequest] that has been executed by an [ImageLoader].
 */
interface Disposable {

    /**
     * The most recent image request job.
     * This field is **not immutable** and can change if the request is replayed.
     */
    val job: Deferred<ImageResult>

    /**
     * Returns 'true' if this disposable's work is complete or cancelling.
     */
    val isDisposed: Boolean

    /**
     * Cancels this disposable's work and releases any held resources.
     */
    fun dispose()
}

/**
 * A disposable for one-shot image requests.
 */
internal class OneShotDisposable(
    override val job: Deferred<ImageResult>
) : Disposable {

    override val isDisposed: Boolean
        get() = !job.isActive

    override fun dispose() {
        if (isDisposed) return
        job.cancel()
    }
}

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
    @Volatile override var job: Deferred<ImageResult>
) : Disposable {

    override val isDisposed: Boolean
        get() = view.requestManager.isDisposed(this)

    override fun dispose() {
        if (isDisposed) return
        view.requestManager.dispose()
    }
}
