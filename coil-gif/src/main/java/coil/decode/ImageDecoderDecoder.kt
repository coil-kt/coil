@file:Suppress("unused")

package coil.decode

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import androidx.core.graphics.decodeDrawable
import androidx.core.util.component1
import androidx.core.util.component2
import coil.annotation.InternalCoilApi
import coil.bitmap.BitmapPool
import coil.drawable.ScaleDrawable
import coil.request.repeatCount
import coil.size.PixelSize
import coil.size.Size
import okio.BufferedSource
import okio.buffer
import okio.sink
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * A [Decoder] that uses [ImageDecoder] to decode GIFs, animated WebPs, and animated HEIFs.
 *
 * NOTE: Animated HEIF files are only supported on API 30 and above.
 */
@RequiresApi(28)
class ImageDecoderDecoder : Decoder {

    override fun handles(source: BufferedSource, mimeType: String?): Boolean {
        return DecodeUtils.isGif(source) ||
            DecodeUtils.isAnimatedWebP(source) ||
            (SDK_INT >= 30 && DecodeUtils.isAnimatedHeif(source))
    }

    @OptIn(InternalCoilApi::class)
    override suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult = withInterruptibleSource(source) { interruptibleSource ->
        var tempFile: File? = null

        try {
            var isSampled = false

            val bufferedSource = interruptibleSource.buffer()
            val decoderSource = if (SDK_INT >= 30) {
                // Buffer the source into memory.
                ImageDecoder.createSource(ByteBuffer.wrap(bufferedSource.use { it.readByteArray() }))
            } else {
                // Work around https://issuetracker.google.com/issues/139371066 by copying the source to a temp file.
                tempFile = createTempFile()
                bufferedSource.use { tempFile.sink().use(it::readAll) }
                ImageDecoder.createSource(tempFile)
            }

            val baseDrawable = decoderSource.decodeDrawable { info, _ ->
                // It's safe to delete the temp file here.
                tempFile?.delete()

                if (size is PixelSize) {
                    val (srcWidth, srcHeight) = info.size
                    val multiplier = DecodeUtils.computeSizeMultiplier(
                        srcWidth = srcWidth,
                        srcHeight = srcHeight,
                        dstWidth = size.width,
                        dstHeight = size.height,
                        scale = options.scale
                    )

                    // Set the target size if the image is larger than the requested dimensions
                    // or the request requires exact dimensions.
                    isSampled = multiplier < 1
                    if (isSampled || !options.allowInexactSize) {
                        val targetWidth = (multiplier * srcWidth).roundToInt()
                        val targetHeight = (multiplier * srcHeight).roundToInt()
                        setTargetSize(targetWidth, targetHeight)
                    }
                }

                allocator = if (options.config == Bitmap.Config.HARDWARE) {
                    ImageDecoder.ALLOCATOR_HARDWARE
                } else {
                    ImageDecoder.ALLOCATOR_SOFTWARE
                }

                memorySizePolicy = if (options.allowRgb565) {
                    ImageDecoder.MEMORY_POLICY_LOW_RAM
                } else {
                    ImageDecoder.MEMORY_POLICY_DEFAULT
                }

                if (options.colorSpace != null) {
                    setTargetColorSpace(options.colorSpace)
                }
            }

            val drawable = if (baseDrawable is AnimatedImageDrawable) {
                baseDrawable.repeatCount = options.parameters.repeatCount() ?: AnimatedImageDrawable.REPEAT_INFINITE

                // Wrap AnimatedImageDrawable in a ScaleDrawable so it always scales to fill its bounds.
                ScaleDrawable(baseDrawable, options.scale)
            } else {
                baseDrawable
            }

            DecodeResult(
                drawable = drawable,
                isSampled = isSampled
            )
        } finally {
            tempFile?.delete()
        }
    }

    companion object {
        const val REPEAT_COUNT_KEY = GifDecoder.REPEAT_COUNT_KEY
    }
}
