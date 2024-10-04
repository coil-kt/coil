package sample.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import coil3.AnimatedImage
import coil3.Canvas
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
import org.jetbrains.skia.Data

class AnimatedSkiaImageDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult? {
        val bytes = source.source().use { it.readByteArray() }
        val codec = Codec.makeFromData(Data.makeFromBytes(bytes))
        return DecodeResult(
            image = AnimatedSkiaImage(codec),
            isSampled = false,
        )
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? = AnimatedSkiaImageDecoder(result.source, options)
    }
}

private class AnimatedSkiaImage(
    private val codec: Codec,
) : AnimatedImage {
    private var invalidateTick by mutableIntStateOf(0)
    private var bitmap: Bitmap? = null
    private var startTime: TimeSource.Monotonic.ValueTimeMark? = null
    private var lastFrameIndex = 0
    private var isDone = false

    override var currentBitmap: Bitmap? = null

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

    private fun Canvas.drawFrame(frameIndex: Int) {
        val bitmap = bitmap ?: Bitmap().apply { allocPixels(codec.imageInfo) }.also { bitmap = it }
        codec.readPixels(bitmap, frameIndex)
        currentBitmap = bitmap
//        check(writePixels(bitmap, 0, 0))
    }

    override fun start() {
        startTime = null
        isDone = false
        invalidateTick++
    }

    override fun stop() {
        isDone = true
    }
}

private val AnimationFrameInfo.safeFrameDuration: Int
    get() = duration.let { if (it <= 0) DEFAULT_FRAME_DURATION else it }

private const val DEFAULT_FRAME_DURATION = 100
