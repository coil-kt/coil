package sample.common

import coil3.ComponentRegistry
import coil3.decode.SvgDecoder

internal actual fun ComponentRegistry.Builder.addPlatformComponents() {
    add(SvgDecoder.Factory())
}
