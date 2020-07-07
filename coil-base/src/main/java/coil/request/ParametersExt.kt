@file:JvmName("ParametersKt")
@file:Suppress("NOTHING_TO_INLINE", "unused")

package coil.request

/** Returns the number of parameters in this object. */
inline fun Parameters.count(): Int = size

/** Return true when the set contains elements. */
inline fun Parameters.isNotEmpty(): Boolean = !isEmpty()

/** Returns the value associated with [key] or null if [key] has no mapping. */
inline operator fun Parameters.get(key: String): Any? = value(key)
