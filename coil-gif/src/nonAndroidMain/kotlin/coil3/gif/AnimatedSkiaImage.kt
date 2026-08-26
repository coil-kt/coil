package coil3.gif

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import coil3.BitmapImage
import coil3.Canvas
import coil3.Image
import coil3.asImage
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
    private val decodeImageInfo: ImageInfo,
    private val outputImageInfo: ImageInfo,
    private val encodedDataSize: Long,
    repeatCount: Int,
    private val animatedTransformation: AnimatedTransformation? = null,
    private val onAnimationStart: (() -> Unit)? = null,
    private val onAnimationEnd: (() -> Unit)? = null,
    bufferedFramesCount: Int,
) : Image {

    private val frameCount = codec.frameCount

    init {
        require(frameCount > 0) { "frameCount must be > 0" }
    }

    private val frameInfos = codec.framesInfo
    internal val frameBufferCapacity = minOf(bufferedFramesCount, frameCount)

    private val decodeLock = SynchronizedObject()
    private val frameLock = SynchronizedObject()
    private val frames = ArrayList<Frame>(frameBufferCapacity)
    private var bufferWindowStart = 0
    private var lastDecodedFrameIndex = NO_FRAME
    private var lastDrawnFrameIndex = NO_FRAME
    private var stoppedFrame: Frame? = null

    private val workingBitmaps = createWorkingBitmaps(
        decodeImageInfo = decodeImageInfo,
        outputImageInfo = outputImageInfo,
        needsOutputBitmap = outputImageInfo != decodeImageInfo || animatedTransformation != null,
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

    private val singleIterationDurationMs = cumulativeFrameDurationsMs.last()

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
            if (throwable !is Exception || !isAnimationStopped) {
                try {
                    frames.forEach { it.image.close() }
                } finally {
                    workingBitmaps.close()
                }
                throw throwable
            }
        }
    }

    override fun draw(canvas: Canvas) {
        // Read this state so Compose observes it and redraws when its value changes.
        @Suppress("UNUSED_VARIABLE")
        val invalidation = invalidateTick

        if (canvas.drawStoppedFrame()) return

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
            if (!canvas.drawFrame(lastFrameIndex)) return
            if (!hasNotifiedAnimationEnd) {
                hasNotifiedAnimationEnd = true
                onAnimationEnd?.invoke()
            }
            return
        }

        val frameIndex = frameIndexAt(elapsedTimeMs)
        setBufferWindowStart(frameIndex)
        if (!canvas.drawFrame(frameIndex)) return
        prefetchFrames(frameIndex)
        scheduleInvalidation(frameIndex, elapsedTimeMs)
    }

    internal val bufferedFrameCount: Int
        get() = synchronized(frameLock) { frames.size }

    internal val isAnimationStopped: Boolean
        get() = synchronized(frameLock) { stoppedFrame != null }

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
            if (stoppedFrame == null) {
                bufferWindowStart = frameIndex
            }
        }
    }

    private fun stopAnimation() {
        val didStop = synchronized(frameLock) {
            if (stoppedFrame != null) {
                false
            } else {
                val retainedFrame = frames.firstOrNull { it.index == lastDrawnFrameIndex }
                    ?: frames.lastOrNull()
                    ?: return@synchronized false
                stoppedFrame = retainedFrame
                bufferWindowStart = retainedFrame.index
                true
            }
        }
        if (didStop) {
            prefetchJob?.cancel()
            invalidationJob?.cancel()
        }
    }

    private fun Canvas.drawStoppedFrame(): Boolean {
        return synchronized(frameLock) {
            val frame = stoppedFrame ?: return@synchronized false
            drawImage(frame.image, left = 0f, top = 0f)
            true
        }
    }

    /** Returns false if decoding failed and the stopped frame was drawn instead. */
    private fun Canvas.drawFrame(frameIndex: Int): Boolean {
        while (true) {
            val drawResult: Boolean? = synchronized(frameLock) {
                val retainedFrame = stoppedFrame
                if (retainedFrame != null) {
                    drawImage(retainedFrame.image, left = 0f, top = 0f)
                    false
                } else {
                    frames.firstOrNull { it.index == frameIndex }?.let { frame ->
                        drawImage(frame.image, left = 0f, top = 0f)
                        lastDrawnFrameIndex = frameIndex
                        true
                    }
                }
            }
            if (drawResult != null) return drawResult

            try {
                ensureFrame(frameIndex)
            } catch (exception: Exception) {
                if (!drawStoppedFrame()) throw exception
                return false
            }
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
                } catch (_: Exception) {
                    return@launch
                }
            }
        }
    }

    private fun scheduleInvalidation(frameIndex: Int, elapsedTimeMs: Long) {
        if (isAnimationStopped) return

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
            if (stoppedFrame != null) return@synchronized true
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
        try {
            synchronized(frameLock) {
                if (stoppedFrame != null) return
                frames.firstOrNull { it.index == frameIndex }?.let { return }
            }

            synchronized(decodeLock) {
                synchronized(frameLock) {
                    if (stoppedFrame != null) return
                    frames.firstOrNull { it.index == frameIndex }?.let { return }
                    if (!isInBufferWindow(frameIndex, bufferWindowStart)) return
                }

                // Preserve the buffered frames until their replacement has decoded successfully.
                val image = decodeFrame(frameIndex)
                var isRetained = false
                try {
                    synchronized(frameLock) {
                        if (stoppedFrame == null &&
                            isInBufferWindow(frameIndex, bufferWindowStart)
                        ) {
                            reserveFrameSlot()
                            frames += Frame(frameIndex, image)
                            isRetained = true
                        }
                    }
                } finally {
                    if (!isRetained) image.close()
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            stopAnimation()
            throw exception
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

        val frameBitmap = outputBitmap?.also(decodeBitmap::scalePixelsTo) ?: decodeBitmap

        frameBitmap.applyTransformation(animatedTransformation)
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

internal fun decodeStaticImage(
    codec: Codec,
    decodeImageInfo: ImageInfo,
    outputImageInfo: ImageInfo,
    animatedTransformation: AnimatedTransformation?,
): BitmapImage {
    val workingBitmaps = createWorkingBitmaps(
        decodeImageInfo = decodeImageInfo,
        outputImageInfo = outputImageInfo,
        needsOutputBitmap = outputImageInfo != decodeImageInfo,
    )
    val decodeBitmap = workingBitmaps.decode
    val outputBitmap = workingBitmaps.output
    var retainedBitmap: Bitmap? = null
    try {
        codec.readPixels(decodeBitmap)
        val resultBitmap = outputBitmap?.also(decodeBitmap::scalePixelsTo) ?: decodeBitmap
        resultBitmap.applyTransformation(animatedTransformation)
        resultBitmap.setImmutable()

        val image = resultBitmap.asImage()
        // Keep the bitmap that backs the returned image open.
        retainedBitmap = resultBitmap
        return image
    } finally {
        workingBitmaps.closeExcept(retainedBitmap)
    }
}

private fun createWorkingBitmaps(
    decodeImageInfo: ImageInfo,
    outputImageInfo: ImageInfo,
    needsOutputBitmap: Boolean,
): WorkingBitmaps {
    val decode = allocateBitmap(decodeImageInfo, "image")
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

private fun Bitmap.scalePixelsTo(destination: Bitmap) {
    checkNotNull(peekPixels()).use { source ->
        checkNotNull(destination.peekPixels()).use { destinationPixels ->
            check(source.scalePixels(destinationPixels, SamplingMode.DEFAULT)) {
                "Unable to resize image frame."
            }
        }
    }
}

private fun Bitmap.applyTransformation(transformation: AnimatedTransformation?) {
    transformation ?: return
    Canvas(this).use(transformation::transform)
}

private class WorkingBitmaps(
    val decode: Bitmap,
    val output: Bitmap?,
) {
    fun close() = closeExcept(retainedBitmap = null)

    fun closeExcept(retainedBitmap: Bitmap?) {
        try {
            if (output !== retainedBitmap) output?.close()
        } finally {
            if (decode !== retainedBitmap) decode.close()
        }
    }
}

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
