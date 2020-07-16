@file:JvmName("Parameters")
@file:Suppress("NOTHING_TO_INLINE", "PackageDirectoryMismatch", "unused")

package coil.extension

import coil.request.Parameters
import coil.request.count as _count
import coil.request.get as _get
import coil.request.isNotEmpty as _isNotEmpty

@Deprecated("Replace `coil.extension.count` with `coil.request.count`.")
inline fun Parameters.count() = _count()

@Deprecated("Replace `coil.extension.isNotEmpty` with `coil.request.isNotEmpty`.")
inline fun Parameters.isNotEmpty() = _isNotEmpty()

@Deprecated("Replace `coil.extension.get` with `coil.request.get`.")
inline operator fun Parameters.get(key: String) = _get(key)
