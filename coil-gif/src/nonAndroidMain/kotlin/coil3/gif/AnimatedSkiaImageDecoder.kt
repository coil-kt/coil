package coil3.gif

import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.util.component1
import coil3.util.component2
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import okio.BufferedSource
import okio.use
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.ImageInfo

/**
 * A [Decoder] that uses Skia to decode animated GIFs and WebPs.
 *
 * @param bufferedFramesCount The maximum number of decoded frames to keep in memory. Must be at
 * least 1. Frames outside the buffer are decoded again as the animation advances.
 */
class AnimatedSkiaImageDecoder(
    private val source: ImageSource,
    private val options: Options,
    private val bufferedFramesCount: Int,
    private val timeSource: TimeSource,
) : Decoder {

    init {
        require(bufferedFramesCount >= 1) { "bufferedFramesCount must be >= 1" }
    }

    override suspend fun decode(): DecodeResult {
        val bytes = source.source().use { it.readByteArray() }
        val data = Data.makeFromBytes(bytes)
        val codec = try {
            Codec.makeFromData(data)
        } finally {
            data.close()
        }

        var ownsCodec = true
        try {
            val frameCount = codec.frameCount
            check(frameCount > 0) { "Animated images must contain at least 1 frame." }

            val sourceImageInfo = codec.imageInfo
            val (outputImageInfo, isSampled) = computeOutputImageInfo(sourceImageInfo, options)
            val decodeImageInfo = computeDecodeImageInfo(sourceImageInfo, outputImageInfo)
            if (frameCount == 1) {
                return DecodeResult(
                    image = decodeStaticImage(
                        codec = codec,
                        decodeImageInfo = decodeImageInfo,
                        outputImageInfo = outputImageInfo,
                        animatedTransformation = options.animatedTransformation,
                    ),
                    isSampled = isSampled,
                )
            }

            val coroutineScope = CoroutineScope(currentCoroutineContext() + SupervisorJob())
            val result = DecodeResult(
                image = AnimatedSkiaImage(
                    codec = codec,
                    coroutineScope = coroutineScope,
                    timeSource = timeSource,
                    decodeImageInfo = decodeImageInfo,
                    outputImageInfo = outputImageInfo,
                    encodedDataSize = bytes.size.toLong(),
                    repeatCount = options.repeatCount,
                    bufferedFramesCount = bufferedFramesCount,
                    animatedTransformation = options.animatedTransformation,
                    onAnimationStart = options.animationStartCallback,
                    onAnimationEnd = options.animationEndCallback,
                ),
                isSampled = isSampled,
            )
            ownsCodec = false
            return result
        } finally {
            if (ownsCodec) codec.close()
        }
    }

    private fun computeOutputImageInfo(
        source: ImageInfo,
        options: Options,
    ): Pair<ImageInfo, Boolean> {
        val (dstWidth, dstHeight) = DecodeUtils.computeDstSize(
            srcWidth = source.width,
            srcHeight = source.height,
            targetSize = options.size,
            scale = options.scale,
            maxSize = options.maxBitmapSize,
        )
        var multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = source.width,
            srcHeight = source.height,
            dstWidth = dstWidth,
            dstHeight = dstHeight,
            scale = options.scale,
            maxSize = options.maxBitmapSize,
        )
        if (options.precision == Precision.INEXACT) {
            multiplier = multiplier.coerceAtMost(1.0)
        }

        val width = (multiplier * source.width).toInt().coerceAtLeast(1)
        val height = (multiplier * source.height).toInt().coerceAtLeast(1)
        return source.withWidthHeight(width, height) to
            (width < source.width || height < source.height)
    }

    private fun computeDecodeImageInfo(
        source: ImageInfo,
        output: ImageInfo,
    ): ImageInfo {
        // Decode downsampled pixels directly, but defer upscaling to the output bitmap.
        return source.withWidthHeight(
            width = minOf(source.width, output.width),
            height = minOf(source.height, output.height),
        )
    }

    /**
     * @param bufferedFramesCount The maximum number of decoded frames to keep in memory. Frames
     * outside the buffer are decoded again as the animation advances.
     * @param timeSource The time source used to advance the animation.
     */
    class Factory(
        private val bufferedFramesCount: Int = DEFAULT_BUFFERED_FRAMES_COUNT,
        private val timeSource: TimeSource = TimeSource.Monotonic,
    ) : Decoder.Factory {

        init {
            require(bufferedFramesCount >= 1) { "bufferedFramesCount must be >= 1" }
        }

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result.source.source())) return null
            return AnimatedSkiaImageDecoder(
                source = result.source,
                options = options,
                bufferedFramesCount = bufferedFramesCount,
                timeSource = timeSource,
            )
        }

        private fun isApplicable(source: BufferedSource): Boolean {
            return DecodeUtils.isGif(source) || DecodeUtils.isAnimatedWebP(source)
        }

        companion object {
            const val DEFAULT_BUFFERED_FRAMES_COUNT = 2
        }
    }

    companion object {
        /**
         * Pass this to `ImageRequest.Builder.repeatCount` to repeat according to the image's
         * encoded loop count.
         */
        const val ENCODED_LOOP_COUNT = -2
    }
}
