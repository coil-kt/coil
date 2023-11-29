package sample.common

import android.os.Build.VERSION.SDK_INT
import coil.ComponentRegistry
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder

internal actual fun ComponentRegistry.Builder.addPlatformComponents() {
    // GIFs
    if (SDK_INT >= 28) {
        add(ImageDecoderDecoder.Factory())
    } else {
        add(GifDecoder.Factory())
    }
    // SVGs
    add(SvgDecoder.Factory())
    // Video frames
    add(VideoFrameDecoder.Factory())
}
