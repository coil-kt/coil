package coil3.util

import android.view.View
import coil3.request.Disposable
import coil3.request.ImageResult
import coil3.request.requestManager

/** Public utility methods for Coil. */
object CoilUtils {

    /**
     * Dispose the request that's attached to this view (if there is one).
     *
     * NOTE: Typically you should use [Disposable.dispose] to cancel requests and clear resources,
     * however this method is provided for convenience.
     *
     * @see Disposable.dispose
     */
    @JvmStatic
    fun dispose(view: View) {
        view.requestManager.dispose()
    }

    /**
     * Get the [ImageResult] of the most recent executed image request that's attached to this view.
     */
    @JvmStatic
    fun result(view: View): ImageResult? {
        return view.requestManager.getResult()
    }
}
