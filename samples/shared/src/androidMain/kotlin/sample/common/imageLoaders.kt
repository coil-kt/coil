package sample.common

import android.os.Build.VERSION.SDK_INT
import coil3.ComponentRegistry
import coil3.decode.AnimatedImageDecoderDecoder
import coil3.decode.GifDecoder
import coil3.decode.SvgDecoder
import coil3.decode.VideoFrameDecoder

internal actual fun newComponentRegistry(): ComponentRegistry {
    val components = ComponentRegistry.Builder()
    if (SDK_INT >= 28) {
        components.add(AnimatedImageDecoderDecoder.Factory())
    } else {
        components.add(GifDecoder.Factory())
    }
    components.add(SvgDecoder.Factory())
    components.add(VideoFrameDecoder.Factory())
    return components.build()
}
