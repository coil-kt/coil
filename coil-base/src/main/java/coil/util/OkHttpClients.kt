@file:JvmName("OkHttpClients")

package coil.util

import android.content.Context
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.OkHttpClient

/**
 * Applies the preferred optimizations for Coil (image loading library) to this given Builder instance.
 */
fun OkHttpClient.Builder.applyCoilOptimizations(context: Context): OkHttpClient.Builder = apply {
    // Create the default image disk cache.
    val cacheDirectory = Utils.getDefaultCacheDirectory(context)
    val cacheSize = Utils.calculateDiskCacheSize(cacheDirectory)
    cache(Cache(cacheDirectory, cacheSize))

    // Don't limit the number of requests by host.
    val dispatcher = Dispatcher().apply {
        maxRequestsPerHost = maxRequests
    }
    dispatcher(dispatcher)
}
