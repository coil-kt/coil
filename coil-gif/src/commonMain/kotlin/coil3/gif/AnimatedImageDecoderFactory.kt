package coil3.gif

import coil3.decode.Decoder

/**
 * Returns a [Decoder.Factory] that can decode animated images, like GIFs.
 * The underlying implementation will depend on the current platform.
 */
expect fun AnimatedImageDecoderFactory(): Decoder.Factory
