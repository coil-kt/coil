package coil3.util

import coil3.annotation.InternalCoilApi
import coil3.decode.SvgDecoder

@InternalCoilApi
internal class SvgDecoderServiceLoaderTarget : DecoderServiceLoaderTarget {
    override fun factory() = SvgDecoder.Factory()
}
