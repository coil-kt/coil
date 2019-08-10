@file:Suppress("unused")

package coil.decode

import android.graphics.ImageDecoder
import android.os.Build.VERSION_CODES.P
import androidx.annotation.RequiresApi
import androidx.core.graphics.decodeDrawable
import coil.bitmappool.BitmapPool
import coil.size.PixelSize
import coil.size.Size
import okio.BufferedSource
import java.nio.ByteBuffer

/**
 * A [Decoder] that uses [ImageDecoder]. This is only used to load GIF and animated WEBP images on Android P and above.
 */
@RequiresApi(P)
class ImageDecoderDecoder : Decoder {

    override fun handles(source: BufferedSource, mimeType: String?): Boolean {
        return DecodeUtils.isGif(source) || DecodeUtils.isAnimatedWebP(source)
    }

    override suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult {
        var isSampled = false
        val decoderSource = source.use {
            ImageDecoder.createSource(ByteBuffer.wrap(it.readByteArray()))
        }
        val drawable = decoderSource.decodeDrawable { info, _ ->
            // Set the target size if the source image is larger than the target.
            if (size is PixelSize && (info.size.width > size.width || info.size.height > size.height)) {
                isSampled = true
                setTargetSize(size.width, size.height)
            }

            if (options.colorSpace != null) {
                setTargetColorSpace(options.colorSpace)
            }

            memorySizePolicy = if (options.allowRgb565) {
                ImageDecoder.MEMORY_POLICY_LOW_RAM
            } else {
                ImageDecoder.MEMORY_POLICY_DEFAULT
            }
        }
        return DecodeResult(
            drawable = drawable,
            isSampled = isSampled
        )
    }
}
