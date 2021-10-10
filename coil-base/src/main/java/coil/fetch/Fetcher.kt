package coil.fetch

import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.decode.ImageSource
import coil.request.Options

/**
 * A [Fetcher] translates data into either an [ImageSource] or a [Drawable].
 *
 * To accomplish this, fetchers fit into one of two types:
 *
 * - Uses the data as a key to fetch bytes from a remote source (e.g. network or disk)
 *   and exposes it as an [ImageSource]. e.g. [HttpUriFetcher]
 * - Reads the data directly and translates it into a [Drawable] (e.g. [BitmapFetcher]).
 */
fun interface Fetcher {

    /**
     * Fetch the data provided by [Factory.create] or return 'null' to delegate to the next
     * [Fetcher] in the component registry.
     */
    suspend fun fetch(): FetchResult?

    fun interface Factory<T : Any> {

        /**
         * Return a [Fetcher] that can fetch [data] or 'null' if this factory cannot create a
         * fetcher for the data.
         *
         * @param data The data to fetch.
         * @param options A set of configuration options for this request.
         * @param imageLoader The [ImageLoader] that's executing this request.
         */
        fun create(data: T, options: Options, imageLoader: ImageLoader): Fetcher?
    }
}
