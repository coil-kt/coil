package coil3.gif

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import coil3.BitmapImage
import coil3.Canvas
import coil3.Image
import coil3.asImage
import coil3.gif.AnimatedImageDecoderUtils.ENCODED_LOOP_COUNT
import coil3.gif.AnimatedImageDecoderUtils.REPEAT_INFINITE
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.jetbrains.skia.AnimationDisposalMode
import org.jetbrains.skia.AnimationFrameInfo
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.impl.use

/**
 * A Skia-backed animated [Image].
 *
 * The animation starts automatically when the image is first drawn. Call [stop] to retain the most
 * recently drawn frame, [start] to restart the animation from its first frame, and [close] to release
 * its native resources early. Once closed, this image cannot be restarted or drawn.
 */
class AnimatedSkiaImage internal constructor(
    private val codec: Codec,
    private val coroutineScope: CoroutineScope,
    private val timeSource: TimeSource,
    private val decodeImageInfo: ImageInfo,
    private val outputImageInfo: ImageInfo,
    private val encodedDataSize: Long,
    bufferedFramesCount: Int,
    repeatCount: Int,
    private val animatedTransformation: AnimatedTransformation? = null,
    private val onAnimationStart: (() -> Unit)? = null,
    private val onAnimationEnd: (() -> Unit)? = null,
) : Image, AutoCloseable {

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
    private var animationState = AnimationState.IDLE
    private var retainedFrame: Frame? = null

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
    private var prefetchJob: Job? = null
    private var prefetchWindowStart = NO_FRAME
    private var invalidationJob: Job? = null
    private var invalidationFrameIndex = NO_FRAME
    private var invalidationIteration = -1L

    // Allocate native pixel buffers after metadata and state so failures there cannot strand them.
    // The initialization block below owns cleanup if predecoding fails.
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

    init {
        try {
            repeat(frameBufferCapacity) { frameIndex ->
                ensureFrame(frameIndex)
            }
        } catch (throwable: Throwable) {
            if (throwable !is Exception || !hasDecodeFailed) {
                try {
                    frames.forEach { it.image.close() }
                } finally {
                    workingBitmaps.close()
                }
                throw throwable
            }
        }
    }

    /**
     * Start the animation, or do nothing if it is already running.
     *
     * Initial playback starts automatically when the image is first drawn.
     * The animation is reset to its first frame when it is next drawn.
     */
    fun start() {
        if (frameCount == 1) return

        val didStart = synchronized(frameLock) {
            when (animationState) {
                AnimationState.IDLE,
                AnimationState.STOPPED,
                -> {
                    animationState = AnimationState.STARTING
                    true
                }
                AnimationState.STARTING,
                AnimationState.RUNNING,
                AnimationState.FAILED,
                AnimationState.CLOSED,
                -> false
            }
        }
        if (!didStart) return

        cancelAnimationJobs()
        invalidateTick++
    }

    /**
     * Stop the animation and retain the most recently drawn frame.
     *
     * Calling this before the first draw prevents automatic playback.
     */
    fun stop() {
        var notifyAnimationEnd = false
        val didStop = synchronized(frameLock) {
            val frame = when (animationState) {
                AnimationState.IDLE -> frames.firstOrNull()
                AnimationState.STARTING ->
                    retainedFrame
                    ?: findBufferedFrameLocked(lastDrawnFrameIndex)
                    ?: frames.firstOrNull()
                AnimationState.RUNNING -> {
                    findBufferedFrameLocked(lastDrawnFrameIndex) ?: frames.lastOrNull()
                }
                AnimationState.STOPPED,
                AnimationState.FAILED,
                AnimationState.CLOSED,
                -> return@synchronized false
            } ?: return@synchronized false

            notifyAnimationEnd = animationState.isRunning
            animationState = AnimationState.STOPPED
            retainedFrame = frame
            bufferWindowStart = frame.index
            animationStartTime = null
            true
        }
        if (!didStop) return

        cancelAnimationJobs()
        if (notifyAnimationEnd) onAnimationEnd?.invoke()
        invalidateTick++
    }

    /** Returns true if this image's animation is currently running. */
    fun isRunning(): Boolean {
        return synchronized(frameLock) { animationState.isRunning }
    }

    /**
     * Release this image's native resources.
     *
     * This method is idempotent. Once closed, this image cannot be restarted or drawn.
     */
    override fun close() {
        val shouldClose = synchronized(frameLock) {
            if (animationState == AnimationState.CLOSED) {
                false
            } else {
                animationState = AnimationState.CLOSED
                retainedFrame = null
                animationStartTime = null
                true
            }
        }
        if (!shouldClose) return

        cancelAnimationJobs()
        coroutineScope.cancel()
        synchronized(decodeLock) {
            val images = synchronized(frameLock) {
                frames.map { it.image }.also {
                    frames.clear()
                    lastDrawnFrameIndex = NO_FRAME
                }
            }
            try {
                images.forEach { it.close() }
            } finally {
                try {
                    workingBitmaps.close()
                } finally {
                    codec.close()
                }
            }
        }
    }

    override fun draw(canvas: Canvas) {
        // Read this state so Compose observes it and redraws when its value changes.
        @Suppress("UNUSED_VARIABLE")
        val invalidation = invalidateTick

        if (frameCount == 1) {
            setBufferWindowStart(0)
            canvas.drawFrame(0)
            return
        }

        when (synchronized(frameLock) { animationState }) {
            AnimationState.IDLE,
            AnimationState.STARTING,
            -> beginAnimation()
            AnimationState.RUNNING -> Unit
            AnimationState.STOPPED,
            AnimationState.FAILED,
            AnimationState.CLOSED,
            -> {
                canvas.drawRetainedFrame()
                return
            }
        }
        if (canvas.drawRetainedFrame()) return

        val startTime = synchronized(frameLock) { animationStartTime } ?: return
        val elapsedTimeMs = startTime.elapsedNow().inWholeMilliseconds.coerceAtLeast(0L)
        val isAnimationComplete = maxIterationCount > 0L &&
            elapsedTimeMs / singleIterationDurationMs >= maxIterationCount

        if (isAnimationComplete) {
            val lastFrameIndex = frameCount - 1
            setBufferWindowStart(lastFrameIndex)
            if (!canvas.drawFrame(lastFrameIndex)) return
            stop()
            return
        }

        val frameIndex = frameIndexAt(elapsedTimeMs)
        setBufferWindowStart(frameIndex)
        if (!canvas.drawFrame(frameIndex)) return
        prefetchFrames(frameIndex)
        scheduleInvalidation(frameIndex)
    }

    internal val bufferedFrameCount: Int
        get() = synchronized(frameLock) { frames.size }

    private val hasDecodeFailed: Boolean
        get() = synchronized(frameLock) { animationState == AnimationState.FAILED }

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
            if (animationState.canDecodeFrames) {
                bufferWindowStart = frameIndex
            }
        }
    }

    private fun beginAnimation() {
        val didBegin = synchronized(frameLock) {
            if (animationState != AnimationState.IDLE &&
                animationState != AnimationState.STARTING
            ) {
                false
            } else {
                animationState = AnimationState.RUNNING
                retainedFrame = null
                bufferWindowStart = 0
                animationStartTime = timeSource.markNow()
                prefetchWindowStart = NO_FRAME
                invalidationFrameIndex = NO_FRAME
                invalidationIteration = -1L
                true
            }
        }
        if (didBegin) onAnimationStart?.invoke()
    }

    private fun stopOnDecodeFailure() {
        var notifyAnimationEnd = false
        val didStop = synchronized(frameLock) {
            if (animationState == AnimationState.FAILED ||
                animationState == AnimationState.CLOSED
            ) {
                false
            } else {
                val frame = findBufferedFrameLocked(lastDrawnFrameIndex) ?: frames.lastOrNull()
                    ?: return@synchronized false
                notifyAnimationEnd = animationState.isRunning
                animationState = AnimationState.FAILED
                retainedFrame = frame
                bufferWindowStart = frame.index
                animationStartTime = null
                true
            }
        }
        if (didStop) {
            cancelAnimationJobs()
            if (notifyAnimationEnd) onAnimationEnd?.invoke()
        }
    }

    private fun cancelAnimationJobs() {
        prefetchJob?.cancel()
        prefetchJob = null
        invalidationJob?.cancel()
        invalidationJob = null
    }

    private fun Canvas.drawRetainedFrame(): Boolean {
        return synchronized(frameLock) {
            when (animationState) {
                AnimationState.STOPPED,
                AnimationState.FAILED,
                -> {
                    retainedFrame?.let { drawImage(it.image, left = 0f, top = 0f) }
                    true
                }
                AnimationState.CLOSED -> true
                AnimationState.IDLE,
                AnimationState.STARTING,
                AnimationState.RUNNING,
                -> false
            }
        }
    }

    /** Returns false if decoding failed and the stopped frame was drawn instead. */
    private fun Canvas.drawFrame(frameIndex: Int): Boolean {
        while (true) {
            val drawResult: Boolean? = synchronized(frameLock) {
                if (!animationState.canDecodeFrames) {
                    retainedFrame?.let { drawImage(it.image, left = 0f, top = 0f) }
                    false
                } else {
                    findBufferedFrameLocked(frameIndex)?.let { frame ->
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
                if (!drawRetainedFrame()) throw exception
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

    private fun scheduleInvalidation(frameIndex: Int) {
        val startTime = synchronized(frameLock) {
            if (animationState.isRunning) animationStartTime else null
        } ?: return
        val elapsedTimeMs = startTime.elapsedNow().inWholeMilliseconds.coerceAtLeast(0L)

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
            if (isRunning()) invalidateTick++
        }
    }

    private fun isBufferWindowComplete(frameIndex: Int): Boolean {
        return synchronized(frameLock) {
            !animationState.canDecodeFrames ||
                (
                    frames.size == frameBufferCapacity &&
                    frames.all { isInBufferWindow(it.index, frameIndex) }
                )
        }
    }

    private fun ensureFrame(frameIndex: Int) {
        try {
            if (!shouldDecodeFrame(frameIndex)) return

            synchronized(decodeLock) {
                if (!shouldDecodeFrame(frameIndex)) return

                // Preserve the buffered frames until their replacement has decoded successfully.
                val image = decodeFrame(frameIndex)
                var isRetained = false
                try {
                    synchronized(frameLock) {
                        if (animationState.canDecodeFrames &&
                            isInBufferWindow(frameIndex, bufferWindowStart)
                        ) {
                            reserveFrameSlotLocked()
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
            stopOnDecodeFailure()
            throw exception
        }
    }

    private fun shouldDecodeFrame(frameIndex: Int): Boolean {
        return synchronized(frameLock) {
            animationState.canDecodeFrames &&
                findBufferedFrameLocked(frameIndex) == null &&
                isInBufferWindow(frameIndex, bufferWindowStart)
        }
    }

    private fun reserveFrameSlotLocked() {
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
            output.updateAlphaType(outputImageInfo.alphaTypeForTransformation(animatedTransformation))
            decodeBitmap.scalePixelsTo(output)
        } ?: decodeBitmap

        frameBitmap.applyTransformation(
            transformation = animatedTransformation,
            inputAlphaType = outputImageInfo.colorAlphaType,
        )
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

    private fun findBufferedFrameLocked(frameIndex: Int): Frame? {
        return frames.firstOrNull { it.index == frameIndex }
    }

    private enum class AnimationState(
        val canDecodeFrames: Boolean,
        val isRunning: Boolean,
    ) {
        IDLE(canDecodeFrames = true, isRunning = false),
        STARTING(canDecodeFrames = true, isRunning = true),
        RUNNING(canDecodeFrames = true, isRunning = true),
        STOPPED(canDecodeFrames = false, isRunning = false),
        FAILED(canDecodeFrames = false, isRunning = false),
        CLOSED(canDecodeFrames = false, isRunning = false),
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
        needsOutputBitmap = outputImageInfo != decodeImageInfo || animatedTransformation != null,
    )
    val decodeBitmap = workingBitmaps.decode
    val outputBitmap = workingBitmaps.output
    var retainedBitmap: Bitmap? = null
    try {
        codec.readPixels(decodeBitmap)
        val resultBitmap = outputBitmap?.also { output ->
            output.updateAlphaType(outputImageInfo.alphaTypeForTransformation(animatedTransformation))
            decodeBitmap.scalePixelsTo(output)
        } ?: decodeBitmap
        resultBitmap.applyTransformation(
            transformation = animatedTransformation,
            inputAlphaType = outputImageInfo.colorAlphaType,
        )
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

private fun Bitmap.applyTransformation(
    transformation: AnimatedTransformation?,
    inputAlphaType: ColorAlphaType,
) {
    transformation ?: return
    val transformedAlphaType = when (Canvas(this).use(transformation::transform)) {
        PixelOpacity.UNCHANGED -> if (inputAlphaType == ColorAlphaType.OPAQUE) {
            ColorAlphaType.OPAQUE
        } else {
            ColorAlphaType.PREMUL
        }
        PixelOpacity.TRANSLUCENT -> ColorAlphaType.PREMUL
        PixelOpacity.OPAQUE -> ColorAlphaType.OPAQUE
    }
    updateAlphaType(transformedAlphaType)
}

private fun Bitmap.updateAlphaType(alphaType: ColorAlphaType) {
    if (imageInfo.colorAlphaType == alphaType) return
    check(setAlphaType(alphaType)) { "Unable to set image alpha type to $alphaType." }
}

private fun ImageInfo.alphaTypeForTransformation(
    transformation: AnimatedTransformation?,
): ColorAlphaType {
    return if (transformation == null) colorAlphaType else ColorAlphaType.PREMUL
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
