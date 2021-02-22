@file:Suppress("unused")

package coil.decode

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import androidx.core.graphics.decodeDrawable
import androidx.core.util.component1
import androidx.core.util.component2
import coil.bitmap.BitmapPool
import coil.drawable.ScaleDrawable
import coil.request.animatedTransformation
import coil.request.animationEndCallback
import coil.request.animationStartCallback
import coil.request.repeatCount
import coil.size.PixelSize
import coil.size.Size
import coil.util.asPostProcessor
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
                tempFile = File.createTempFile("tmp", null, null)
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

                isUnpremultipliedRequired = !options.premultipliedAlpha

                postProcessor = options.parameters.animatedTransformation()?.asPostProcessor()
            }

            val drawable = if (baseDrawable is AnimatedImageDrawable) {
                baseDrawable.repeatCount = options.parameters.repeatCount() ?: AnimatedImageDrawable.REPEAT_INFINITE

                // Set the start and end animation callbacks if any one is supplied through the request.
                if (options.parameters.animationStartCallback() != null ||
                    options.parameters.animationEndCallback() != null) {
                    baseDrawable.registerAnimationCallback(object : Animatable2.AnimationCallback() {
                        override fun onAnimationStart(drawable: Drawable?) {
                            options.parameters.animationStartCallback()?.invoke()
                        }

                        override fun onAnimationEnd(drawable: Drawable?) {
                            options.parameters.animationEndCallback()?.invoke()
                        }
                    })
                }

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
        const val ANIMATED_TRANSFORMATION_KEY = GifDecoder.ANIMATED_TRANSFORMATION_KEY
        const val ANIMATION_START_CALLBACK_KEY = GifDecoder.ANIMATION_START_CALLBACK_KEY
        const val ANIMATION_END_CALLBACK_KEY = GifDecoder.ANIMATION_END_CALLBACK_KEY
    }
}
