@file:JvmName("AnimatedImageDecoderFactory")

package coil3.gif

import coil3.decode.Decoder
import kotlin.jvm.JvmName

@JvmName("create")
actual fun AnimatedImageDecoderFactory(): Decoder.Factory =
    AnimatedSkiaImageDecoder.Factory()
