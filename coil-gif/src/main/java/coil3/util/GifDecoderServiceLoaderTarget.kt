package coil3.util

import android.os.Build.VERSION.SDK_INT
import coil3.decode.AnimatedImageDecoderDecoder
import coil3.decode.GifDecoder

internal class GifDecoderServiceLoaderTarget : DecoderServiceLoaderTarget {
    override fun factory() = if (SDK_INT >= 28) {
        AnimatedImageDecoderDecoder.Factory()
    } else {
        GifDecoder.Factory()
    }
}
