package coil3.gif

import coil3.decode.Decoder
import coil3.test.utils.AbstractGifDecoderTest

class GifDecoderTest : AbstractGifDecoderTest() {

    override val decoderFactory: Decoder.Factory =
        AnimatedSkiaImageDecoder.Factory(timeSource = testTimeSource)
}
