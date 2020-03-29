package coil.util

import android.content.Context
import android.view.View
import coil.annotation.ExperimentalCoilApi
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

    /** Cancel any in progress requests attached to [view] and clear any associated resources. */
    @ExperimentalCoilApi
    @JvmStatic
    fun clear(view: View) {
        view.requestManager.clearCurrentRequest()
    }
}
