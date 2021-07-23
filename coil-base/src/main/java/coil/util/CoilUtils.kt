@file:Suppress("UNUSED_PARAMETER")

package coil.util

import android.content.Context
import android.view.View
import coil.network.imageLoaderDiskCache // ktlint-disable no-unused-imports
import coil.request.Disposable
import coil.request.ImageResult
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.io.File

/** Public utility methods for Coil. */
object CoilUtils {

    // Tracks 'okhttp3.Cache.ENTRY_BODY' which is private.
    private const val ENTRY_BODY = 1

    /**
     * Create an OkHttp disk cache with a reasonable default size and location.
     *
     * @see OkHttpClient.Builder.imageLoaderDiskCache
     */
    @JvmStatic
    fun createDiskCache(context: Context): Cache {
        val cacheDirectory = Utils.getDefaultDiskCacheDirectory(context)
        val cacheSize = Utils.calculateDiskCacheSize(cacheDirectory)
        return Cache(cacheDirectory, cacheSize)
    }

    /**
     * Get the [File] for a [url] in [diskCache].
     *
     * NOTE: This function **does not** imply that the returned [File] exists. Callers should
     * always check [File.exists] before using it. Additionally, if the file does exist it can be
     * deleted at any moment (especially if new entries are actively being added to [diskCache]).
     */
    @JvmStatic
    fun getDiskCacheFile(diskCache: Cache, url: HttpUrl): File {
        return diskCache.directory.resolve("${Cache.key(url)}.$ENTRY_BODY")
    }

    /**
     * Dispose the request attached to this view (if there is one).
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
     * Get the [ImageResult] of the most recent executed image request attached to this view.
     */
    @JvmStatic
    fun result(view: View): ImageResult? {
        return view.requestManager.getResult()
    }

    @Deprecated(
        message = "Migrate to 'OkHttpClient.Builder.imageLoaderDiskCache'.",
        level = DeprecationLevel.ERROR // Temporary migration aid.
    )
    @JvmStatic
    fun createDefaultCache(context: Context): Cache = unsupported()
}
