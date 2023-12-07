package coil3.key

import coil3.request.Options

/**
 * An interface to convert data of type [T] into a string key for the memory cache.
 */
fun interface Keyer<T : Any> {

    /**
     * Convert [data] into a string key. Return 'null' if this keyer cannot convert [data].
     *
     * @param data The data to convert.
     * @param options The options for this request.
     */
    fun key(data: T, options: Options): String?
}
