package sample.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import coil3.Canvas
import coil3.Image
import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlin.time.TimeSource
import okio.use
import org.jetbrains.skia.AnimationFrameInfo
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorInfo
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Data
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Image as SkiaImage

class AnimatedSkiaImageDecoder(
    private val source: ImageSource,
    private val options: Options,
    private val prerenderFrames: Boolean = true,
) : Decoder {

    override suspend fun decode(): DecodeResult? {
        val bytes = source.source().use { it.readByteArray() }
        val codec = Codec.makeFromData(Data.makeFromBytes(bytes))
        return DecodeResult(
            image = AnimatedSkiaImage(codec, prerenderFrames),
            isSampled = false,
        )
    }

    class Factory(
        private val prerenderFrames: Boolean = false,
    ) : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? = AnimatedSkiaImageDecoder(result.source, options, prerenderFrames)
    }
}

private class AnimatedSkiaImage(
    private val codec: Codec,
    prerenderFrames: Boolean,
) : Image {
    private val imageInfo = ImageInfo(
        colorInfo = ColorInfo(
            colorType = ColorType.BGRA_8888,
            alphaType = ColorAlphaType.UNPREMUL,
            colorSpace = ColorSpace.sRGB,
        ),
        width = codec.width,
        height = codec.height,
    )
    private val bitmap = Bitmap().apply { allocPixels(codec.imageInfo) }
    private val frames = Array(codec.frameCount) { index ->
        if (prerenderFrames) decodeFrame(index) else null
    }

    private var invalidateTick by mutableIntStateOf(0)
    private var startTime: TimeSource.Monotonic.ValueTimeMark? = null
    private var lastFrameIndex = 0
    private var isDone = false

    override val size: Long
        get() {
            var size = codec.imageInfo.computeMinByteSize().toLong()
            if (size <= 0L) {
                // Estimate 4 bytes per pixel.
                size = 4L * codec.width * codec.height
            }
            return size.coerceAtLeast(0)
        }

    override val width: Int
        get() = codec.width

    override val height: Int
        get() = codec.height

    override val shareable: Boolean
        get() = false

    override fun draw(canvas: Canvas) {
        val totalFrames = codec.framesInfo.size
        if (totalFrames == 0) {
            return
        }

        if (totalFrames == 1) {
            canvas.drawFrame(0)
            return
        }

        if (isDone) {
            canvas.drawFrame(lastFrameIndex)
            return
        }

        val startTime = startTime ?: TimeSource.Monotonic.markNow().also { startTime = it }
        val elapsedTime = startTime.elapsedNow().inWholeMilliseconds
        var durationMillis = 0
        var frameIndex = totalFrames - 1
        for ((index, frame) in codec.framesInfo.withIndex()) {
            if (durationMillis > elapsedTime) {
                frameIndex = (index - 1).coerceAtLeast(0)
                break
            }
            durationMillis += frame.safeFrameDuration
        }
        lastFrameIndex = frameIndex
        isDone = frameIndex == (totalFrames - 1)

        canvas.drawFrame(frameIndex)

        if (!isDone) {
            // Increment this value to force the image to be redrawn.
            invalidateTick++
        }
    }

    private fun decodeFrame(frameIndex: Int): ByteArray {
        codec.readPixels(bitmap, frameIndex)
        return bitmap.readPixels(imageInfo, imageInfo.minRowBytes)!!
    }

    private fun Canvas.drawFrame(frameIndex: Int) {
        val frame = frames[frameIndex] ?: decodeFrame(frameIndex).also { frames[frameIndex] = it }
        drawImage(SkiaImage.makeRaster(imageInfo, frame, imageInfo.minRowBytes), 0f, 0f)
    }
}

private val AnimationFrameInfo.safeFrameDuration: Int
    get() = duration.let { if (it <= 0) DEFAULT_FRAME_DURATION else it }

private const val DEFAULT_FRAME_DURATION = 100
