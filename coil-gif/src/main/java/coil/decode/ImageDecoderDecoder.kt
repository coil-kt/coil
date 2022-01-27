package coil.decode

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.AnimatedImageDrawable.REPEAT_INFINITE
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import androidx.core.graphics.decodeDrawable
import androidx.core.util.component1
import androidx.core.util.component2
import coil.ImageLoader
import coil.drawable.ScaleDrawable
import coil.fetch.SourceResult
import coil.request.Options
import coil.request.animatedTransformation
import coil.request.animationEndCallback
import coil.request.animationStartCallback
import coil.request.repeatCount
import coil.size.pxOrElse
import coil.util.animatable2CallbackOf
import coil.util.asPostProcessor
import coil.util.isHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okio.BufferedSource
import okio.buffer
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * A [Decoder] that uses [ImageDecoder] to decode GIFs, animated WebPs, and animated HEIFs.
 *
 * NOTE: Animated HEIF files are only supported on API 30 and above.
 *
 * @param enforceMinimumFrameDelay If true, rewrite a GIF's frame delay to a default value if
 *  it is below a threshold. See https://github.com/coil-kt/coil/issues/540 for more info.
 */
@RequiresApi(28)
class ImageDecoderDecoder @JvmOverloads constructor(
    private val source: ImageSource,
    private val options: Options,
    private val enforceMinimumFrameDelay: Boolean = true
) : Decoder {

    override suspend fun decode(): DecodeResult {
        var isSampled = false
        val drawable = runInterruptible {
            var imageDecoder: ImageDecoder? = null
            val wrappedSource = wrapImageSource(source)
            try {
                wrappedSource.toImageDecoderSource().decodeDrawable { info, _ ->
                    // Capture the image decoder to manually close it later.
                    imageDecoder = this

                    // Configure the output image's size.
                    val (srcWidth, srcHeight) = info.size
                    val dstWidth = options.size.width.pxOrElse { srcWidth }
                    val dstHeight = options.size.height.pxOrElse { srcHeight }
                    if (srcWidth > 0 && srcHeight > 0 &&
                        (srcWidth != dstWidth || srcHeight != dstHeight)) {
                        val multiplier = DecodeUtils.computeSizeMultiplier(
                            srcWidth = srcWidth,
                            srcHeight = srcHeight,
                            dstWidth = dstWidth,
                            dstHeight = dstHeight,
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

                    // Configure any other attributes.
                    configureImageDecoderProperties()
                }
            } finally {
                imageDecoder?.close()
                wrappedSource.close()
            }
        }
        return DecodeResult(wrapDrawable(drawable), isSampled)
    }

    private fun wrapImageSource(source: ImageSource): ImageSource {
        return if (enforceMinimumFrameDelay && DecodeUtils.isGif(source.source())) {
            // Wrap the source to rewrite its frame delay as it's read.
            val rewritingSource = FrameDelayRewritingSource(source.source())
            ImageSource(rewritingSource.buffer(), options.context)
        } else {
            source
        }
    }

    private fun ImageSource.toImageDecoderSource(): ImageDecoder.Source {
        val file = fileOrNull()
        if (file != null) {
            return ImageDecoder.createSource(file)
        }

        val metadata = metadata
        if (metadata is AssetMetadata) {
            return ImageDecoder.createSource(options.context.assets, metadata.fileName)
        }
        if (metadata is ContentMetadata) {
            return ImageDecoder.createSource(options.context.contentResolver, metadata.uri)
        }
        if (metadata is ResourceMetadata && metadata.packageName == options.context.packageName) {
            return ImageDecoder.createSource(options.context.resources, metadata.resId)
        }

        return when {
            SDK_INT >= 31 -> ImageDecoder.createSource(source().readByteArray())
            SDK_INT == 30 -> ImageDecoder.createSource(ByteBuffer.wrap(source().readByteArray()))
            // https://issuetracker.google.com/issues/139371066
            else -> ImageDecoder.createSource(file())
        }
    }

    private fun ImageDecoder.configureImageDecoderProperties() {
        allocator = if (options.config.isHardware) {
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

    private suspend fun wrapDrawable(baseDrawable: Drawable): Drawable {
        if (baseDrawable !is AnimatedImageDrawable) {
            return baseDrawable
        }

        baseDrawable.repeatCount = options.parameters.repeatCount() ?: REPEAT_INFINITE

        // Set the start and end animation callbacks if any one is supplied through the request.
        val onStart = options.parameters.animationStartCallback()
        val onEnd = options.parameters.animationEndCallback()
        if (onStart != null || onEnd != null) {
            // Animation callbacks must be set on the main thread.
            withContext(Dispatchers.Main.immediate) {
                baseDrawable.registerAnimationCallback(animatable2CallbackOf(onStart, onEnd))
            }
        }

        // Wrap AnimatedImageDrawable in a ScaleDrawable so it always scales to fill its bounds.
        return ScaleDrawable(baseDrawable, options.scale)
    }

    @RequiresApi(28)
    class Factory @JvmOverloads constructor(
        private val enforceMinimumFrameDelay: Boolean = true
    ) : Decoder.Factory {

        override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
            if (!isApplicable(result.source.source())) return null
            return ImageDecoderDecoder(result.source, options, enforceMinimumFrameDelay)
        }

        private fun isApplicable(source: BufferedSource): Boolean {
            return DecodeUtils.isGif(source) ||
                DecodeUtils.isAnimatedWebP(source) ||
                (SDK_INT >= 30 && DecodeUtils.isAnimatedHeif(source))
        }

        override fun equals(other: Any?) = other is Factory

        override fun hashCode() = javaClass.hashCode()
    }
}
