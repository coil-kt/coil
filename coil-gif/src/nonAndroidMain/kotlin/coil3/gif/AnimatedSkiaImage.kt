package coil3.gif

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import coil3.Canvas
import coil3.Image
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.jetbrains.skia.AnimationDisposalMode
import org.jetbrains.skia.AnimationFrameInfo
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.impl.use

internal class AnimatedSkiaImage(
    private val codec: Codec,
    private val coroutineScope: CoroutineScope,
    private val timeSource: TimeSource,
    private val outputImageInfo: ImageInfo,
    private val encodedDataSize: Long,
    repeatCount: Int,
    private val animatedTransformation: AnimatedTransformation? = null,
    private val onAnimationStart: (() -> Unit)? = null,
    private val onAnimationEnd: (() -> Unit)? = null,
    bufferedFramesCount: Int,
) : Image {

    private val frameCount = codec.frameCount
    private val frameInfos = codec.framesInfo
    internal val frameBufferCapacity = minOf(bufferedFramesCount, frameCount)

    private val decodeLock = SynchronizedObject()
    private val frameLock = SynchronizedObject()
    private val frames = ArrayList<Frame>(frameBufferCapacity)
    private var bufferWindowStart = 0
    private var lastDecodedFrameIndex = NO_FRAME
    private var failedFrameIndex = NO_FRAME
    private var decodeFailure: Throwable? = null

    private val workingBitmaps = createWorkingBitmaps(
        decodeImageInfo = codec.imageInfo,
        outputImageInfo = outputImageInfo,
        needsOutputBitmap = outputImageInfo != codec.imageInfo || animatedTransformation != null,
    )
    private val decodeBitmap = workingBitmaps.decode
    private val outputBitmap = workingBitmaps.output

    override val size = encodedDataSize
        .saturatedAdd(decodeBitmap.byteSize())
        .saturatedAdd(outputBitmap?.byteSize() ?: 0L)
        .saturatedAdd(outputImageInfo.byteSize().saturatedMultiply(frameBufferCapacity))

    override val width: Int
        get() = outputImageInfo.width

    override val height: Int
        get() = outputImageInfo.height

    override val shareable: Boolean
        get() = false

    /** The number of times the animation is played, or 0 if it repeats forever. */
    internal val maxIterationCount = when (repeatCount) {
        ENCODED_LOOP_COUNT -> codec.repetitionCount.toIterationCount()
        REPEAT_INFINITE -> 0L
        else -> repeatCount.toLong() + 1L
    }

    internal val frameDurationsMs = List(frameCount) { index ->
        frameInfos.getOrNull(index)?.safeFrameDuration ?: DEFAULT_FRAME_DURATION
    }

    private val cumulativeFrameDurationsMs = LongArray(frameCount).also { durations ->
        var total = 0L
        frameDurationsMs.forEachIndexed { index, duration ->
            total = total.saturatedAdd(duration.toLong())
            durations[index] = total
        }
    }

    internal val singleIterationDurationMs =
        cumulativeFrameDurationsMs.lastOrNull() ?: 0L

    private var invalidateTick by mutableIntStateOf(0)
    private var animationStartTime: TimeMark? = null
    private var hasNotifiedAnimationEnd = false
    private var prefetchJob: Job? = null
    private var prefetchWindowStart = NO_FRAME
    private var invalidationJob: Job? = null
    private var invalidationFrameIndex = NO_FRAME
    private var invalidationIteration = -1L

    init {
        try {
            repeat(frameBufferCapacity) { frameIndex ->
                ensureFrame(frameIndex)
            }
        } catch (throwable: Throwable) {
            frames.forEach { it.image.close() }
            outputBitmap?.close()
            decodeBitmap.close()
            throw throwable
        }
    }

    override fun draw(canvas: Canvas) {
        @Suppress("UNUSED_VARIABLE")
        val invalidation = invalidateTick

        if (frameCount == 0) return

        if (frameCount == 1) {
            setBufferWindowStart(0)
            canvas.drawFrame(0)
            return
        }

        val startTime = animationStartTime ?: timeSource.markNow().also {
            animationStartTime = it
            onAnimationStart?.invoke()
        }
        val elapsedTimeMs = startTime.elapsedNow().inWholeMilliseconds.coerceAtLeast(0L)
        val isAnimationComplete = maxIterationCount > 0L &&
            elapsedTimeMs / singleIterationDurationMs >= maxIterationCount

        if (isAnimationComplete) {
            prefetchJob?.cancel()
            invalidationJob?.cancel()
            val lastFrameIndex = frameCount - 1
            setBufferWindowStart(lastFrameIndex)
            canvas.drawFrame(lastFrameIndex)
            if (!hasNotifiedAnimationEnd) {
                hasNotifiedAnimationEnd = true
                onAnimationEnd?.invoke()
            }
            return
        }

        val frameIndex = frameIndexAt(elapsedTimeMs)
        setBufferWindowStart(frameIndex)
        canvas.drawFrame(frameIndex)
        prefetchFrames(frameIndex)
        scheduleInvalidation(frameIndex, elapsedTimeMs)
    }

    internal val bufferedFrameCount: Int
        get() = synchronized(frameLock) { frames.size }

    private fun frameIndexAt(elapsedTimeMs: Long): Int {
        val iterationElapsedTimeMs = elapsedTimeMs % singleIterationDurationMs
        var low = 0
        var high = cumulativeFrameDurationsMs.lastIndex
        while (low < high) {
            val middle = (low + high) ushr 1
            if (iterationElapsedTimeMs < cumulativeFrameDurationsMs[middle]) {
                high = middle
            } else {
                low = middle + 1
            }
        }
        return low
    }

    private fun setBufferWindowStart(frameIndex: Int) {
        synchronized(frameLock) {
            bufferWindowStart = frameIndex
        }
    }

    private fun Canvas.drawFrame(frameIndex: Int) {
        while (true) {
            val wasDrawn = synchronized(frameLock) {
                frames.firstOrNull { it.index == frameIndex }?.let { frame ->
                    drawImage(frame.image, left = 0f, top = 0f)
                    true
                } ?: false
            }
            if (wasDrawn) return
            ensureFrame(frameIndex)
        }
    }

    private fun prefetchFrames(frameIndex: Int) {
        if (frameBufferCapacity <= 1 || isBufferWindowComplete(frameIndex)) return
        if (prefetchWindowStart == frameIndex && prefetchJob?.isActive == true) return

        prefetchJob?.cancel()
        prefetchWindowStart = frameIndex
        prefetchJob = coroutineScope.launch {
            for (offset in 1 until frameBufferCapacity) {
                currentCoroutineContext().ensureActive()
                try {
                    ensureFrame(nextFrameIndex(frameIndex, offset))
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Throwable) {
                    return@launch
                }
            }
        }
    }

    private fun scheduleInvalidation(frameIndex: Int, elapsedTimeMs: Long) {
        val iteration = elapsedTimeMs / singleIterationDurationMs
        if (invalidationFrameIndex == frameIndex &&
            invalidationIteration == iteration &&
            invalidationJob?.isActive == true
        ) {
            return
        }

        invalidationJob?.cancel()
        invalidationFrameIndex = frameIndex
        invalidationIteration = iteration
        val iterationElapsedTimeMs = elapsedTimeMs % singleIterationDurationMs
        val delayMs = (cumulativeFrameDurationsMs[frameIndex] - iterationElapsedTimeMs)
            .coerceAtLeast(1L)
        invalidationJob = coroutineScope.launch {
            delay(delayMs.milliseconds)
            invalidateTick++
        }
    }

    private fun isBufferWindowComplete(frameIndex: Int): Boolean {
        return synchronized(frameLock) {
            if (failedFrameIndex != NO_FRAME && isInBufferWindow(failedFrameIndex, frameIndex)) {
                return@synchronized true
            }
            for (offset in 0 until frameBufferCapacity) {
                val expectedFrameIndex = nextFrameIndex(frameIndex, offset)
                if (frames.none { it.index == expectedFrameIndex }) {
                    return@synchronized false
                }
            }
            true
        }
    }

    private fun ensureFrame(frameIndex: Int) {
        synchronized(frameLock) {
            frames.firstOrNull { it.index == frameIndex }?.let { return }
            if (failedFrameIndex == frameIndex) throw checkNotNull(decodeFailure)
        }

        synchronized(decodeLock) {
            synchronized(frameLock) {
                frames.firstOrNull { it.index == frameIndex }?.let { return }
                if (failedFrameIndex == frameIndex) throw checkNotNull(decodeFailure)
                if (!isInBufferWindow(frameIndex, bufferWindowStart)) return
                reserveFrameSlot()
            }

            val image = try {
                decodeFrame(frameIndex)
            } catch (throwable: Throwable) {
                synchronized(frameLock) {
                    failedFrameIndex = frameIndex
                    decodeFailure = throwable
                }
                throw throwable
            }

            synchronized(frameLock) {
                if (isInBufferWindow(frameIndex, bufferWindowStart)) {
                    frames += Frame(frameIndex, image)
                } else {
                    image.close()
                }
            }
        }
    }

    private fun reserveFrameSlot() {
        if (frames.size < frameBufferCapacity) return
        val index = frames.indexOfFirst { !isInBufferWindow(it.index, bufferWindowStart) }
            .takeIf { it >= 0 }
            ?: 0
        frames.removeAt(index).image.close()
    }

    private fun decodeFrame(frameIndex: Int): SkiaImage {
        codec.readPixels(
            bitmap = decodeBitmap,
            frame = frameIndex,
            priorFrame = priorFrameFor(frameIndex),
        )
        lastDecodedFrameIndex = frameIndex

        val frameBitmap = outputBitmap?.also { output ->
            checkNotNull(decodeBitmap.peekPixels()).use { source ->
                checkNotNull(output.peekPixels()).use { destination ->
                    check(source.scalePixels(destination, SamplingMode.DEFAULT)) {
                        "Unable to resize animated image frame."
                    }
                }
            }
        } ?: decodeBitmap

        if (animatedTransformation != null) {
            Canvas(frameBitmap).use(animatedTransformation::transform)
        }
        return SkiaImage.makeFromBitmap(frameBitmap)
    }

    private fun priorFrameFor(frameIndex: Int): Int {
        if (frameIndex <= 0 || lastDecodedFrameIndex !in 0..<frameIndex) return NO_FRAME
        val requiredFrame = frameInfos.getOrNull(frameIndex)?.requiredFrame ?: return NO_FRAME
        if (requiredFrame == NO_FRAME || lastDecodedFrameIndex < requiredFrame) return NO_FRAME
        val priorDisposal = frameInfos.getOrNull(lastDecodedFrameIndex)?.disposalMethod
        return if (priorDisposal == AnimationDisposalMode.RESTORE_PREVIOUS) {
            NO_FRAME
        } else {
            lastDecodedFrameIndex
        }
    }

    private fun isInBufferWindow(frameIndex: Int, windowStart: Int): Boolean {
        if (frameCount == 0) return false
        val distance = if (frameIndex >= windowStart) {
            frameIndex.toLong() - windowStart
        } else {
            frameCount.toLong() - windowStart + frameIndex
        }
        return distance < frameBufferCapacity
    }

    private fun nextFrameIndex(frameIndex: Int, offset: Int): Int {
        return ((frameIndex.toLong() + offset) % frameCount).toInt()
    }

    private class Frame(
        val index: Int,
        val image: SkiaImage,
    )
}

private fun createWorkingBitmaps(
    decodeImageInfo: ImageInfo,
    outputImageInfo: ImageInfo,
    needsOutputBitmap: Boolean,
): WorkingBitmaps {
    val decode = allocateBitmap(decodeImageInfo, "animated image")
    val output = try {
        if (needsOutputBitmap) allocateBitmap(outputImageInfo, "output image") else null
    } catch (throwable: Throwable) {
        decode.close()
        throw throwable
    }
    return WorkingBitmaps(decode, output)
}

private fun allocateBitmap(imageInfo: ImageInfo, description: String): Bitmap {
    val bitmap = Bitmap()
    try {
        check(bitmap.allocPixels(imageInfo)) { "Unable to allocate $description pixels." }
        return bitmap
    } catch (throwable: Throwable) {
        bitmap.close()
        throw throwable
    }
}

private class WorkingBitmaps(
    val decode: Bitmap,
    val output: Bitmap?,
)

private val AnimationFrameInfo.safeFrameDuration: Int
    get() = if (duration <= 0) DEFAULT_FRAME_DURATION else duration

private fun Int.toIterationCount(): Long {
    return if (this < 0) 0L else toLong() + 1L
}

private fun Bitmap.byteSize(): Long {
    return imageInfo.byteSize()
}

private fun ImageInfo.byteSize(): Long {
    val computedSize = computeMinByteSize().toLong()
    return if (computedSize > 0L) {
        computedSize
    } else {
        (4L * width * height).coerceAtLeast(0L)
    }
}

private fun Long.saturatedAdd(other: Long): Long {
    return if (Long.MAX_VALUE - this < other) Long.MAX_VALUE else this + other
}

private fun Long.saturatedMultiply(other: Int): Long {
    return if (other == 0 || this == 0L) {
        0L
    } else if (this > Long.MAX_VALUE / other) {
        Long.MAX_VALUE
    } else {
        this * other
    }
}

private const val NO_FRAME = -1
private const val DEFAULT_FRAME_DURATION = 100
