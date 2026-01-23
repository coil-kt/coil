package coil3.gif

import android.os.Build.VERSION.SDK_INT
import coil3.decode.Decoder

actual fun AnimatedImageDecoderFactory(): Decoder.Factory =
    if (SDK_INT >= 28) {
        AnimatedImageDecoder.Factory()
    } else {
        GifDecoder.Factory()
    }
