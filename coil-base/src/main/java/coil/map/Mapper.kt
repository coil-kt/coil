@file:Suppress("unused")

package coil.map

import coil.fetch.Fetcher

/**
 * An interface to convert data of type [T] into [V].
 *
 * Use this to map custom data types to a type that can be handled by a [Fetcher].
 */
interface Mapper<T : Any, V : Any> {

    /** Return true if this can convert [data]. */
    fun handles(data: T): Boolean = true

    /** Convert [data] into [V]. */
    fun map(data: T): V
}
