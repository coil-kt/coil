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

    /**
     * The number of times the animation must be played through.
     */
    val maxIterationCount: Int
        get() = codec.repetitionCount + 1

    /**
     * The duration of each frame, in order, in milliseconds.
     */
    val frameDurationsMs: List<Int> by lazy {
        codec.framesInfo.map { it.safeFrameDuration }
    }

    /**
     * The total duration of a single iteration of the animation in milliseconds.
     */
    val singleIterationDurationMs: Int by lazy {
        frameDurationsMs.sum()
    }

    /**
     * A temporary [Bitmap] used to decode individual frames.
     */
    private val tempBitmap = Bitmap().apply { allocPixels(codec.imageInfo) }

    /**
     * An array of decoded frames.
     *
     * Initially load `bufferedFramesCount` frames; the rest will be loaded in the background.
     */
    private val frames = Array(codec.frameCount) { index ->
        if (index in 0..<bufferedFramesCount) decodeFrame(index) else null
    }

    private var bufferFramesJob: Job? = null

    /**
     * A value that is incremented to force the image to be invalidated by Compose.
     */
    private var invalidateTick by mutableIntStateOf(0)

    /**
     * The time when the animation first started playing. Used to calculate the current frame to be drawn.
     */
    private var animationStartTime: TimeMark? = null

    private var hasNotifiedAnimationEnd = false

    override fun draw(canvas: Canvas) {
        if (codec.frameCount == 0) {
            return
        }

        if (codec.frameCount == 1) {
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

                // Everything has been buffered, we can free some memory.
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

        val isAnimationComplete =
            maxIterationCount > 0 && totalElapsedTimeMs >= singleIterationDurationMs * maxIterationCount

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
        )

        canvas.drawFrame(frameIndexToDraw)

        if (!isAnimationComplete) {
            invalidateTick++
        }
    }

    /**
     * Returns the index of the frame to draw based on the current time.
     *
     * If the animation is complete, the index of the last frame is returned.
     *
     * @param frameDurationsMs The list of frame durations in milliseconds.
     */
    private fun getFrameIndexToDraw(
        frameDurationsMs: List<Int>,
        singleIterationDurationMs: Int,
        totalElapsedTimeMs: Long,
    ): Int {
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
