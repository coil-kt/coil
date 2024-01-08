package coil3.svg.internal

import coil3.svg.SvgDecoder
import coil3.util.DecoderServiceLoaderTarget

internal class SvgDecoderServiceLoaderTarget : DecoderServiceLoaderTarget {
    override fun factory() = SvgDecoder.Factory()
}
