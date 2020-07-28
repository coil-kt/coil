@file:Suppress("UNCHECKED_CAST")

package coil.fetch

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import coil.bitmap.BitmapPool
import coil.decode.Options
import coil.memory.MemoryCache
import coil.size.Size
import okio.BufferedSource

/**
 * A [Fetcher] translates data into either a [BufferedSource] or a [Drawable].
 *
 * To accomplish this, fetchers fit into one of two types:
 *
 * - Uses the data as a key to fetch bytes from a remote source (e.g. network or disk)
 *   and exposes it as a [BufferedSource]. e.g. [HttpUrlFetcher]
 * - Reads the data directly and translates it into a [Drawable]. e.g. [BitmapFetcher]
 */
interface Fetcher<T : Any> {

    /**
     * Return true if this can load [data].
     */
    fun handles(data: T): Boolean = true

    /**
     * Compute the memory cache key for [data].
     *
     * Items with the same cache key will be treated as equivalent by the [MemoryCache].
     *
     * Returning null will prevent the result of [fetch] from being added to the memory cache.
     */
    fun key(data: T): String?

    /**
     * Load the [data] into memory. Perform any necessary fetching operations.
     *
     * @param pool A [BitmapPool] which can be used to request [Bitmap] instances.
     * @param data The data to load.
     * @param size The requested dimensions for the image.
     * @param options A set of configuration options for this request.
     */
    suspend fun fetch(
        pool: BitmapPool,
        data: T,
        size: Size,
        options: Options
    ): FetchResult
}
