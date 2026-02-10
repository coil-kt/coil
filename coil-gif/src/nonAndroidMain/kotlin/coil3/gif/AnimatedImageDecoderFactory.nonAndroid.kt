package coil3.gif

import coil3.decode.Decoder

actual fun AnimatedImageDecoderFactory(): Decoder.Factory =
    AnimatedSkiaImageDecoder.Factory()
