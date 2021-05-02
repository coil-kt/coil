@file:Suppress("UNCHECKED_CAST")

package coil.fetch

import android.graphics.drawable.Drawable
import coil.decode.ImageSource
import coil.decode.Options
import coil.memory.MemoryCache
import okio.BufferedSource

/**
 * A [Fetcher] translates data into either an [ImageSource] or a [Drawable].
 *
 * To accomplish this, fetchers fit into one of two types:
 *
 * - Uses the data as a key to fetch bytes from a remote source (e.g. network or disk)
 *   and exposes it as either a [BufferedSource]. e.g. [HttpFetcher]
 * - Reads the data directly and translates it into a [Drawable]. e.g. [BitmapFetcher]
 */
interface Fetcher<T : Any> {

    /**
     * Compute the memory cache key for [data].
     *
     * Items with the same cache key will be treated as equivalent by the [MemoryCache].
     *
     * Returning null will prevent the result of [fetch] from being added to the memory cache.
     */
    fun cacheKey(data: T): String?

    /**
     * Load the [data] into memory. Perform any necessary fetching operations.
     *
     * @param data The data to load.
     * @param options A set of configuration options for this request.
     */
    suspend fun fetch(data: T, options: Options): FetchResult

    fun interface Factory<T : Any> {

        fun create(data: T, options: Options): Fetcher<T>
    }
}
