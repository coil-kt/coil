package coil3.util

import coil3.decode.GifDecoder

internal class GifDecoderServiceLoaderTarget : DecoderServiceLoaderTarget {
    override fun factory() = GifDecoder.Factory()
}
