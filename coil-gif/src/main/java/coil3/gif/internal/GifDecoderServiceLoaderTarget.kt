package coil3.gif.internal

import android.os.Build.VERSION.SDK_INT
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.util.DecoderServiceLoaderTarget

internal class GifDecoderServiceLoaderTarget : DecoderServiceLoaderTarget {
    override fun factory() = if (SDK_INT >= 28) {
        AnimatedImageDecoder.Factory()
    } else {
        GifDecoder.Factory()
    }
}
