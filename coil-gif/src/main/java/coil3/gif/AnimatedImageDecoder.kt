package coil3.gif

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import androidx.core.graphics.decodeDrawable
import androidx.core.util.component1
import androidx.core.util.component2
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.AssetMetadata
import coil3.decode.ByteBufferMetadata
import coil3.decode.ContentMetadata
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.decode.ResourceMetadata
import coil3.fetch.SourceFetchResult
import coil3.gif.internal.animatable2CallbackOf
import coil3.gif.internal.asPostProcessor
import coil3.gif.internal.maybeWrapImageSourceToRewriteFrameDelay
import coil3.request.Options
import coil3.request.allowRgb565
import coil3.request.bitmapConfig
import coil3.request.colorSpace
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.size.ScaleDrawable
import coil3.toAndroidUri
import coil3.util.component1
import coil3.util.component2
import coil3.util.isHardware
import java.nio.ByteBuffer
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okio.BufferedSource
import okio.FileSystem

/**
 * A [Decoder] that uses [ImageDecoder] to decode GIFs, animated WebPs, and animated HEIFs.
 *
 * NOTE: Animated HEIF files are only supported on API 30 and above.
 *
 * @param enforceMinimumFrameDelay If true, rewrite a GIF's frame delay to a default value if
 *  it is below a threshold. See https://github.com/coil-kt/coil/issues/540 for more info.
 */
@RequiresApi(28)
class AnimatedImageDecoder(
    private val source: ImageSource,
    private val options: Options,
    // https://android.googlesource.com/platform/frameworks/base/+/2be87bb707e2c6d75f668c4aff6697b85fbf5b15
    private val enforceMinimumFrameDelay: Boolean = SDK_INT < 34,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        var isSampled = false
        val drawable = runInterruptible {
            maybeWrapImageSourceToRewriteFrameDelay(source, enforceMinimumFrameDelay).use { source ->
                source.toImageDecoderSource().decodeDrawable { info, _ ->
                    // Configure the output image's size.
                    val (srcWidth, srcHeight) = info.size
                    val (dstWidth, dstHeight) = DecodeUtils.computeDstSize(
                        srcWidth = srcWidth,
                        srcHeight = srcHeight,
                        targetSize = options.size,
                        scale = options.scale,
                        maxSize = options.maxBitmapSize,
                    )
                    if (srcWidth > 0 && srcHeight > 0 &&
                        (srcWidth != dstWidth || srcHeight != dstHeight)) {
                        val multiplier = DecodeUtils.computeSizeMultiplier(
                            srcWidth = srcWidth,
                            srcHeight = srcHeight,
                            dstWidth = dstWidth,
                            dstHeight = dstHeight,
                            scale = options.scale,
                        )

                        // Set the target size if the image is larger than the requested dimensions
                        // or the request requires exact dimensions.
                        isSampled = multiplier < 1
                        if (isSampled || options.precision == Precision.EXACT) {
                            val targetWidth = (multiplier * srcWidth).roundToInt()
                            val targetHeight = (multiplier * srcHeight).roundToInt()
                            setTargetSize(targetWidth, targetHeight)
                        }
                    }

                    // Configure any other attributes.
                    configureImageDecoderProperties()
                }
            }
        }
        return DecodeResult(
            image = wrapDrawable(drawable).asImage(),
            isSampled = isSampled,
        )
    }

    private fun ImageSource.toImageDecoderSource(): ImageDecoder.Source {
        if (fileSystem == FileSystem.SYSTEM) {
            val file = fileOrNull()
            if (file != null && fileSystem == FileSystem.SYSTEM) {
                return ImageDecoder.createSource(file.toFile())
            }
        }

        val metadata = metadata
        if (metadata is AssetMetadata) {
            return ImageDecoder.createSource(options.context.assets, metadata.filePath)
        }
        if (metadata is ContentMetadata) {
            return if (SDK_INT >= 29) {
                // ImageDecoder will seek inner fd to startOffset.
                ImageDecoder.createSource { metadata.assetFileDescriptor }
            } else {
                ImageDecoder.createSource(options.context.contentResolver, metadata.uri.toAndroidUri())
            }
        }
        if (metadata is ResourceMetadata && metadata.packageName == options.context.packageName) {
            return ImageDecoder.createSource(options.context.resources, metadata.resId)
        }
        if (metadata is ByteBufferMetadata) {
            val isDirect = metadata.byteBuffer.isDirect
            if (isDirect || SDK_INT >= 30) return ImageDecoder.createSource(metadata.byteBuffer)
        }

        val bytebuffer = source.source().use { it.squashToDirectByteBuffer() }
        return ImageDecoder.createSource(bytebuffer)
    }

    private fun ImageDecoder.configureImageDecoderProperties() {
        allocator = if (options.bitmapConfig.isHardware) {
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
        postProcessor = options.animatedTransformation?.asPostProcessor()
    }

    private suspend fun wrapDrawable(baseDrawable: Drawable): Drawable {
        if (baseDrawable !is AnimatedImageDrawable) {
            return baseDrawable
        }

        baseDrawable.repeatCount = options.repeatCount

        // Set the start and end animation callbacks if any one is supplied through the request.
        val onStart = options.animationStartCallback
        val onEnd = options.animationEndCallback
        if (onStart != null || onEnd != null) {
            // Animation callbacks must be set on the main thread.
            withContext(Dispatchers.Main.immediate) {
                baseDrawable.registerAnimationCallback(animatable2CallbackOf(onStart, onEnd))
            }
        }

        // Wrap AnimatedImageDrawable in a ScaleDrawable so it always scales to fill its bounds.
        return ScaleDrawable(baseDrawable, options.scale)
    }

    class Factory(
        // https://android.googlesource.com/platform/frameworks/base/+/2be87bb707e2c6d75f668c4aff6697b85fbf5b15
        private val enforceMinimumFrameDelay: Boolean = SDK_INT < 34,
    ) : Decoder.Factory {

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result.source.source())) return null
            return AnimatedImageDecoder(result.source, options, enforceMinimumFrameDelay)
        }

        private fun isApplicable(source: BufferedSource): Boolean {
            return DecodeUtils.isGif(source) ||
                DecodeUtils.isAnimatedWebP(source) ||
                (SDK_INT >= 30 && DecodeUtils.isAnimatedHeif(source))
        }
    }
}

internal fun BufferedSource.squashToDirectByteBuffer(): ByteBuffer {
    // Squash bytes to BufferedSource inner buffer then we know total byteCount.
    request(Long.MAX_VALUE)

    val byteBuffer = ByteBuffer.allocateDirect(buffer.size.toInt())
    while (!buffer.exhausted()) buffer.read(byteBuffer)
    byteBuffer.flip()
    return byteBuffer
}
