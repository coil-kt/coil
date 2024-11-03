package coil3.gif

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import coil3.Canvas
import coil3.Image
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.skia.AnimationFrameInfo
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec

internal class AnimatedSkiaImage(
    private val codec: Codec,
    private val coroutineScope: CoroutineScope,
    private val animatedTransformation: AnimatedTransformation? = null,
    private val onAnimationStart: (() -> Unit)? = null,
    private val onAnimationEnd: (() -> Unit)? = null,
    bufferedFramesCount: Int,
) : Image {

    private val tempBitmap = Bitmap().apply { allocPixels(codec.imageInfo) }

    private val frames = Array(codec.frameCount) { index ->
        if (index in 0..<bufferedFramesCount) decodeFrame(index) else null
    }

    private var invalidateTick by mutableIntStateOf(0)

    private var currentRepetitionStartTime: TimeSource.Monotonic.ValueTimeMark? = null
    private var currentRepetitionCount = 0
    private var lastDrawnFrameIndex = 0
    private var isAnimationComplete = false

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

    private var bufferFramesJob: Job? = null

    private var hasNotifiedAnimationStart = false
    private var hasNotifiedAnimationEnd = false

    override fun draw(canvas: Canvas) {
        if (codec.frameCount == 0) {
            // The image is empty, nothing to draw.
            return
        }

        if (codec.frameCount == 1) {
            // This is a static image, simply draw it.
            canvas.drawFrame(0)
            return
        }

        // Buffer the next frames in the background.
        if (bufferFramesJob == null || bufferFramesJob?.isCancelled == true) {
            bufferFramesJob = coroutineScope.launch(Dispatchers.Default) {
                frames.forEachIndexed { index, bytes ->
                    if (bytes == null) {
                        frames[index] = decodeFrame(index)
                    }
                }

                // Everything has been buffered.
                tempBitmap.close()
            }
        }

        if (!hasNotifiedAnimationStart) {
            onAnimationStart?.invoke()
            hasNotifiedAnimationStart = true
        }

        if (isAnimationComplete) {
            // The animation is complete, freeze on the last frame.
            canvas.drawFrame(lastDrawnFrameIndex)

            if (!hasNotifiedAnimationEnd) {
                onAnimationEnd?.invoke()
                hasNotifiedAnimationEnd = true
            }

            return
        }

        val startTime = currentRepetitionStartTime
            ?: TimeSource.Monotonic.markNow().also { currentRepetitionStartTime = it }
        val elapsedTime = startTime.elapsedNow().inWholeMilliseconds

        var accumulatedDuration = 0
        var frameIndexToDraw = codec.frameCount - 1

        // Find the right frame to draw based on the elapsed time.
        for ((index, frame) in codec.framesInfo.withIndex()) {
            if (accumulatedDuration > elapsedTime) {
                frameIndexToDraw = (index - 1).coerceAtLeast(0)
                break
            }

            accumulatedDuration += frame.safeFrameDuration
        }

        // Remember the last frame we drew; the next time we draw, we'll start from here.
        lastDrawnFrameIndex = frameIndexToDraw

        // Check if we've reached the last frame of the last repetition. If so, we're done.
        isAnimationComplete = codec.repetitionCount in 1..currentRepetitionCount &&
            frameIndexToDraw == (codec.frameCount - 1)

        canvas.drawFrame(frameIndexToDraw)

        // We still need to wait for the last frame's duration before we start with the next repetition.
        val drewLastFrame = frameIndexToDraw == codec.frameCount - 1
        val lastFrameDuration = codec.framesInfo[frameIndexToDraw].safeFrameDuration
        val hasLastFrameDurationElapsed = elapsedTime >= accumulatedDuration + lastFrameDuration

        if (!isAnimationComplete && drewLastFrame && hasLastFrameDurationElapsed) {
            // We've reached the last frame of the current repetition, but we can still loop.
            // Reset the state and start over from the first frame.
            lastDrawnFrameIndex = 0
            currentRepetitionCount++
            currentRepetitionStartTime = null
        }

        if (!isAnimationComplete) {
            // Increment this value to force the image to be redrawn.
            invalidateTick++
        }
    }

    private fun decodeFrame(frameIndex: Int): ByteArray {
        if (tempBitmap.isClosed) {
            throw IllegalStateException("Cannot decode frame: the bitmap is closed.")
        }

        codec.readPixels(tempBitmap, frameIndex)
        return tempBitmap.readPixels(
            dstInfo = codec.imageInfo,
            dstRowBytes = codec.imageInfo.minRowBytes,
        )!!
    }

    private fun Canvas.drawFrame(frameIndex: Int) {
        animatedTransformation?.transform(this)

        frames[frameIndex]?.let { frame ->
            drawImage(
                image = org.jetbrains.skia.Image.makeRaster(
                    imageInfo = codec.imageInfo,
                    bytes = frame,
                    rowBytes = codec.imageInfo.minRowBytes,
                ),
                left = 0f,
                top = 0f,
            )
        }
    }
}

private val AnimationFrameInfo.safeFrameDuration: Int
    get() = duration.let { if (it <= 0) DEFAULT_FRAME_DURATION else it }

private const val DEFAULT_FRAME_DURATION = 100
