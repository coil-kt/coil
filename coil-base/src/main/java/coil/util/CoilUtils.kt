package coil.util

import android.content.Context
import android.view.View
import coil.request.Metadata
import coil.request.RequestDisposable
import okhttp3.Cache

/** Public utility methods for Coil. */
object CoilUtils {

    /** Create an OkHttp disk cache with a reasonable default size and location. */
    @JvmStatic
    fun createDefaultCache(context: Context): Cache {
        val cacheDirectory = Utils.getDefaultCacheDirectory(context)
        val cacheSize = Utils.calculateDiskCacheSize(cacheDirectory)
        return Cache(cacheDirectory, cacheSize)
    }

    /**
     * Cancel any in progress requests attached to [view] and clear any associated resources.
     *
     * NOTE: Typically you should use [RequestDisposable.dispose] to clear any associated resources,
     * however this method is provided for convenience.
     */
    @JvmStatic
    fun clear(view: View) {
        view.requestManager.clearCurrentRequest()
    }

    @JvmStatic
    fun metadata(view: View): Metadata? {
        return view.requestManager.metadata
    }
}
