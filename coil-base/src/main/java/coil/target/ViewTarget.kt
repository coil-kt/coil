package coil.target

import android.graphics.drawable.Drawable
import android.view.View
import androidx.lifecycle.LifecycleObserver

/**
 * A [Target] with an associated [View].
 *
 * If the loaded [Drawable] will only be used with one [View], prefer this to [Target].
 *
 * Unlike [Target]s, [ViewTarget]s can have their lifecycle methods called multiple times.
 *
 * Optionally, [ViewTarget]s can be declared as [LifecycleObserver]s. They are automatically registered when the request
 * starts and unregistered when the request is disposed.
 */
interface ViewTarget<T : View> : Target {

    /**
     * The [View] used by this [Target].
     */
    val view: T
}
