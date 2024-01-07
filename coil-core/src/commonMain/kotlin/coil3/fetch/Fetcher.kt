package coil3.fetch

import coil3.Image
import coil3.ImageLoader
import coil3.decode.ImageSource
import coil3.request.Options

/**
 * A [Fetcher] translates data (e.g. URI, file, etc.) into a [FetchResult].
 *
 * To accomplish this, fetchers fit into one of two types:
 *
 * - Uses the data as a key to fetch bytes from a remote source (e.g. network, disk)
 *   and exposes it as an [ImageSource].
 * - Reads the data directly and translates it into an [Image].
 */
fun interface Fetcher {

    /**
     * Fetch the data provided by [Factory.create] or return 'null' to delegate to the next
     * [Factory] in the component registry.
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
        fun create(
            data: T,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher?
    }
}
