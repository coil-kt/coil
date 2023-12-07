package coil3.map

import coil3.fetch.Fetcher
import coil3.request.Options

/**
 * An interface to convert data of type [T] into [V].
 *
 * Use this to map custom data types to a type that can be handled by a [Fetcher].
 */
fun interface Mapper<T : Any, V : Any> {

    /**
     * Convert [data] into [V]. Return 'null' if this mapper cannot convert [data].
     *
     * @param data The data to convert.
     * @param options The options for this request.
     */
    fun map(data: T, options: Options): V?
}
