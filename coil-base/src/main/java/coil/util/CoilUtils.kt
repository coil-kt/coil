package coil.util

import android.content.Context
import okhttp3.Cache

/** Public utility methods for Coil. */
object CoilUtils {

    /** Create an OkHttp disk cache with a reasonable default size and location. */
    fun createDefaultCache(context: Context): Cache {
        val cacheDirectory = Utils.getDefaultCacheDirectory(context)
        val cacheSize = Utils.calculateDiskCacheSize(cacheDirectory)
        return Cache(cacheDirectory, cacheSize)
    }
}
