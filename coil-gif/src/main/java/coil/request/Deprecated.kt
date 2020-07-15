@file:Suppress("PackageDirectoryMismatch", "unused")
@file:JvmName("Gifs")

package coil.extension

import coil.request.ImageRequest
import coil.request.Parameters
import coil.request.repeatCount as _repeatCount

@Deprecated("Replace `coil.extension.repeatCount` with `coil.request.repeatCount`.")
fun ImageRequest.Builder.repeatCount(repeatCount: Int) = _repeatCount(repeatCount)

@Deprecated("Replace `coil.extension.repeatCount` with `coil.request.repeatCount`.")
fun Parameters.repeatCount() = _repeatCount()
