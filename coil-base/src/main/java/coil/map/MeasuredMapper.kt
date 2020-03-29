@file:Suppress("unused")

package coil.map

import coil.size.Size
import coil.target.Target

/**
 * An interface to convert data with type [T] into [V].
 * Unlike [Mapper]s, [MeasuredMapper] must wait for the [Target] to be measured.
 * This can cause cached Drawables to not be set synchronously.
 *
 * Prefer implementing [Mapper] if you do not need to need to know the size of the [Target].
 *
 * @see Mapper
 */
interface MeasuredMapper<T : Any, V : Any> {

    /** Return true if this can convert [data]. */
    fun handles(data: T): Boolean = true

    /** Convert [data] into [V]. */
    fun map(data: T, size: Size): V
}
