package coil3.network

import coil3.request.Options

/**
 * A strategy for generating unique keys to deduplicate concurrent network requests.
 *
 * Requests with the same key will be deduplicated - if a request is already in flight
 * for a given key, subsequent requests with the same key will wait for the first request
 * to complete and reuse its result.
 */
fun interface RequestKeyGenerator {
    /**
     * Generate a unique key for the given [url] and [options].
     *
     * @param url The URL being requested.
     * @param options The request options.
     * @return A unique string key for this request. Requests with the same key will be deduplicated.
     */
    fun generate(url: String, options: Options): String

    companion object {
        /**
         * Default implementation that uses only the URL as the key.
         * This means all requests to the same URL will be deduplicated,
         * regardless of headers, method, body, etc.
         */
        val DEFAULT = RequestKeyGenerator { url, _ -> url }
    }
}