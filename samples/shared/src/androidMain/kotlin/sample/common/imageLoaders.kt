package sample.common

import android.os.Build.VERSION.SDK_INT
import coil3.ComponentRegistry
import coil3.decode.GifDecoder
import coil3.decode.ImageDecoderDecoder
import coil3.decode.VideoFrameDecoder

internal actual fun ComponentRegistry.Builder.addPlatformComponents() {
    // GIFs
    if (SDK_INT >= 28) {
        add(ImageDecoderDecoder.Factory())
    } else {
        add(GifDecoder.Factory())
    }
    // Video frames
    add(VideoFrameDecoder.Factory())
}
