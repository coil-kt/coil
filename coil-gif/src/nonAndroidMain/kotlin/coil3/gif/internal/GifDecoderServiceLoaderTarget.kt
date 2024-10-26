package coil3.gif.internal

import coil3.gif.AnimatedSkiaImageDecoder
import coil3.util.DecoderServiceLoaderTarget

internal class GifDecoderServiceLoaderTarget : DecoderServiceLoaderTarget {
    override fun factory() = AnimatedSkiaImageDecoder.Factory()
}
