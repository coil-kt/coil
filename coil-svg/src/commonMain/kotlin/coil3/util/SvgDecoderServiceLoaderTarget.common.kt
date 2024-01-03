package coil3.util

import coil3.decode.SvgDecoder

internal class SvgDecoderServiceLoaderTarget : DecoderServiceLoaderTarget {
    override fun factory() = SvgDecoder.Factory()
}
