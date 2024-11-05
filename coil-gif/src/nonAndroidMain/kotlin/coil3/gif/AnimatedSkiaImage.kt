package coil3.gif

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import coil3.Canvas
import coil3.Image
import kotlin.time.TimeMark
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
    private val timeSource: TimeSource,
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

    private var animationStartTime: TimeMark? = null

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

    private var hasNotifiedAnimationEnd = false

    override fun draw(canvas: Canvas) {
        if (codec.frameCount == 0) {
            // The image is empty, nothing to draw.
            return
        }

        if (codec.frameCount == 1) {
            // This is a static image, simply draw it.
            canvas.drawFrame(frameIndex = 0)
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

        if (animationStartTime == null) {
            onAnimationStart?.invoke()
        }

        // Remember the time when the animation first started playing.
        val animationStartTime = animationStartTime
            ?: timeSource.markNow().also { animationStartTime = it }

        val totalElapsedTimeMs = animationStartTime.elapsedNow().inWholeMilliseconds

        val frameDurationsMs = codec.framesInfo.map { it.safeFrameDuration }
        val singleIterationDurationMs = frameDurationsMs.sum()

        val isAnimationComplete =
            codec.repetitionCount > 0 && totalElapsedTimeMs > singleIterationDurationMs * codec.repetitionCount

        if (isAnimationComplete) {
            // The animation is complete, freeze on the last frame.
            canvas.drawFrame(
                frameIndex = codec.framesInfo.lastIndex,
            )

            if (!hasNotifiedAnimationEnd) {
                onAnimationEnd?.invoke()
                hasNotifiedAnimationEnd = true
            }

            return
        }

        val frameIndexToDraw = getFrameIndexToDraw(
            frameDurationsMs = frameDurationsMs,
            singleIterationDurationMs = singleIterationDurationMs,
            totalElapsedTimeMs = totalElapsedTimeMs,
            repetitionCount = codec.repetitionCount,
        )

        canvas.drawFrame(frameIndexToDraw)

        if (!isAnimationComplete) {
            // Increment this value to force the image to be redrawn.
            invalidateTick++
        }
    }

    /**
     * Returns the index of the frame to draw based on the current time.
     *
     * If the animation is complete, the index of the last frame is returned.
     *
     * @param frameDurationsMs The list of frame durations in milliseconds.
     * @param repetitionCount The number of times the animation should repeat, or -1 for infinite.
     */
    private fun getFrameIndexToDraw(
        frameDurationsMs: List<Int>,
        singleIterationDurationMs: Int,
        totalElapsedTimeMs: Long,
        repetitionCount: Int,
    ): Int {
        val currentIteration = totalElapsedTimeMs / singleIterationDurationMs
        if (repetitionCount in 1..currentIteration) {
            return frameDurationsMs.lastIndex
        }

        val currentIterationElapsedTimeMs = totalElapsedTimeMs % singleIterationDurationMs

        return getFrameIndexToDrawWithinIteration(
            frameDurationsMs = frameDurationsMs,
            elapsedTimeMs = currentIterationElapsedTimeMs,
        )
    }

    /**
     * Returns the index of the frame to draw within the current iteration.
     *
     * @param frameDurationsMs The list of frame durations in milliseconds.
     * @param elapsedTimeMs The elapsed time in milliseconds since the start of the iteration.
     */
    private fun getFrameIndexToDrawWithinIteration(
        frameDurationsMs: List<Int>,
        elapsedTimeMs: Long,
    ): Int {
        var accumulatedDuration = 0

        for ((index, frameDuration) in frameDurationsMs.withIndex()) {
            if (accumulatedDuration > elapsedTimeMs) {
                return (index - 1).coerceAtLeast(0)
            }

            accumulatedDuration += frameDuration
        }

        return frameDurationsMs.lastIndex
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
