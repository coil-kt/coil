package coil3.target

import android.view.View
import androidx.lifecycle.LifecycleObserver

/**
 * A [Target] with an associated [View]. Prefer this to [Target] if the given drawables will only
 * be used by [view].
 *
 * Optionally, [ViewTarget]s can implement [LifecycleObserver]. They are automatically registered
 * when the request starts and unregistered when the request completes.
 */
interface ViewTarget<T : View> : Target {

    /**
     * The [View] used by this [Target]. This field should be immutable.
     */
    val view: T
}
