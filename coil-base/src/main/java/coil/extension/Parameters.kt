@file:JvmName("Parameters")
@file:Suppress("NOTHING_TO_INLINE", "unused")

package coil.extension

import coil.request.Parameters

inline fun Parameters.isNotEmpty(): Boolean = !isEmpty()

inline operator fun Parameters.get(key: String): Any? = value(key)
