@file:JvmName("Videos")
@file:Suppress("PackageDirectoryMismatch", "unused")

package coil.extension

import coil.request.ImageRequest
import coil.request.Parameters
import coil.request.videoFrameMicros as _videoFrameMicros
import coil.request.videoFrameMillis as _videoFrameMillis
import coil.request.videoFrameOption as _videoFrameOption

@Deprecated("Replace `coil.extension.videoFrameMillis` with `coil.request.videoFrameMillis`.")
fun ImageRequest.Builder.videoFrameMillis(frameMillis: Long) = _videoFrameMillis(frameMillis)

@Deprecated("Replace `coil.extension.videoFrameMicros` with `coil.request.videoFrameMicros`.")
fun ImageRequest.Builder.videoFrameMicros(frameMicros: Long) = _videoFrameMicros(frameMicros)

@Deprecated("Replace `coil.extension.videoFrameOption` with `coil.request.videoFrameOption`.")
fun ImageRequest.Builder.videoFrameOption(option: Int) = _videoFrameOption(option)

@Deprecated("Replace `coil.extension.videoFrameMicros` with `coil.request.videoFrameMicros`.")
fun Parameters.videoFrameMicros() = _videoFrameMicros()

@Deprecated("Replace `coil.extension.videoFrameOption` with `coil.request.videoFrameOption`.")
fun Parameters.videoFrameOption() = _videoFrameOption()
