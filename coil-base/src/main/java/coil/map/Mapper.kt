@file:Suppress("unused")

package coil.map

import coil.fetch.Fetcher

/**
 * An interface to convert data of type [T] into [V].
 *
 * Use this to map custom data types to a type that can be handled by a [Fetcher].
 */
public interface Mapper<T : Any, V : Any> {

    /** Return true if this can convert [data]. */
    public fun handles(data: T): Boolean = true

    /** Convert [data] into [V]. */
    public fun map(data: T): V
}
