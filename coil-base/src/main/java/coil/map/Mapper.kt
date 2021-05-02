package coil.map

import coil.fetch.Fetcher

/**
 * An interface to convert data of type [T] into [V].
 *
 * Use this to map custom data types to a type that can be handled by a [Fetcher].
 */
fun interface Mapper<T : Any, V : Any> {

    /**
     * Convert [data] into [V]. Return 'null' if this mapper is not applicable for [data].
     */
    fun map(data: T): V?
}
