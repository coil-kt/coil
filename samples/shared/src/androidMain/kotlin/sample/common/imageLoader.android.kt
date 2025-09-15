package sample.common

import coil3.ImageLoader
import coil3.request.allowHardware
import coil3.request.maxBitmapSize
import coil3.size.Size

internal actual fun ImageLoader.Builder.platformSpecificConfig() = apply {
    allowHardware(false)
    maxBitmapSize(Size.ORIGINAL)
}
