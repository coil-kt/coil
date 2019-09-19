@file:Suppress("unused")

package coil.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build.VERSION_CODES.P
import androidx.annotation.RequiresApi
import androidx.core.graphics.decodeDrawable
import coil.bitmappool.BitmapPool
import coil.size.PixelSize
import coil.size.Size
import okio.BufferedSource
import okio.buffer
import okio.sink

/**
 * A [Decoder] that uses [ImageDecoder] to decode GIFs and animated WebPs on Android P and above.
 */
@RequiresApi(P)
class ImageDecoderDecoder(private val context: Context) : Decoder {

    override fun handles(source: BufferedSource, mimeType: String?): Boolean {
        return DecodeUtils.isGif(source) || DecodeUtils.isAnimatedWebP(source)
    }

    override suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult {
        val tempFile = createTempFile(directory = context.cacheDir)

        try {
            var isSampled = false

            // Work around https://issuetracker.google.com/issues/139371066 by copying the source to a temp file.
            source.use { tempFile.sink().buffer().writeAll(it) }
            val decoderSource = ImageDecoder.createSource(tempFile)

            val drawable = decoderSource.decodeDrawable { info, _ ->
                // It's safe to delete the temp file here.
                tempFile.delete()

                // Set the target size if the source image is larger than the target.
                if (size is PixelSize && info.size.run { width > size.width || height > size.height }) {
                    isSampled = true
                    setTargetSize(size.width, size.height)
                }

                if (options.config != Bitmap.Config.HARDWARE) {
                    allocator = ImageDecoder.ALLOCATOR_SOFTWARE
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
        } finally {
            tempFile.delete()
        }
    }
}
