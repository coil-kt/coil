@file:Suppress("UNUSED_PARAMETER")

package coil.util

import android.content.Context
import android.view.View
import coil.request.Disposable
import coil.request.ImageResult
import okhttp3.Cache

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

    @Deprecated(
        message = "ImageLoaders no longer (and should not) use OkHttp's disk cache. " +
            "Use 'ImageLoader.Builder.diskCache' to configure a custom disk cache.",
        level = DeprecationLevel.ERROR // Temporary migration aid.
    )
    @JvmStatic
    fun createDefaultCache(context: Context): Cache = unsupported()
}
