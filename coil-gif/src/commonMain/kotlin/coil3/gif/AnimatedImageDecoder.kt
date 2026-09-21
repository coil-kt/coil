@file:JvmName("AnimatedImageDecoderFactory")

package coil3.gif

import coil3.decode.Decoder
import kotlin.jvm.JvmName

/**
 * Returns a [Decoder.Factory] that can decode animated images, like GIFs.
 * The underlying implementation will depend on the current platform.
 */
@JvmName("create")
expect fun AnimatedImageDecoderFactory(): Decoder.Factory
