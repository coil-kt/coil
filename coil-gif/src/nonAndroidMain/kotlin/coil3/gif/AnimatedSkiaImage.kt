package coil3.gif

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import coil3.Canvas
import coil3.Image
import coil3.gif.AnimatedImageDecoderUtils.ENCODED_LOOP_COUNT
import coil3.gif.AnimatedImageDecoderUtils.REPEAT_INFINITE
import coil3.gif.internal.FrameBitmapPool
import coil3.gif.internal.WorkingBitmaps
import coil3.gif.internal.byteSize
import coil3.gif.internal.safeFrameDuration
import coil3.gif.internal.scalePixelsTo
import coil3.util.IntPair
import coil3.util.component1
import coil3.util.component2
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.jetbrains.skia.AnimationDisposalMode
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.ImageInfo

/**
 * A Skia-backed animated [Image].
 *
 * The animation starts automatically when the image is first drawn. Call [stop] to retain the most
 * recently drawn frame, [start] to restart the animation from its first frame, and [close] to release
 * its native resources early. Once closed, this image cannot be restarted or drawn.
 */
class AnimatedSkiaImage internal constructor(
    private val codec: Codec,
    private val frameCount: Int,
    private val coroutineScope: CoroutineScope,
    private val timeSource: TimeSource,
    decodeImageInfo: ImageInfo,
    private val outputImageInfo: ImageInfo,
    encodedDataSize: Long,
    bufferedFramesCount: Int,
    repeatCount: Int,
    animatedTransformation: AnimatedTransformation? = null,
    private val onAnimationStart: (() -> Unit)? = null,
    private val onAnimationEnd: (() -> Unit)? = null,
) : Image, AutoCloseable {

    init {
        require(frameCount > 0) { "frameCount must be > 0" }
        require(bufferedFramesCount >= 1) { "bufferedFramesCount must be >= 1" }
    }

    // Keep only the metadata needed for timing and compositing, not an object per frame.
    private val requiredFrameIndices = IntArray(frameCount)
    private val restorePreviousFrames = BooleanArray(frameCount)

    private val decodeLock = SynchronizedObject()
    private val frameLock = SynchronizedObject()
    private val bufferedFrames = arrayOfNulls<Frame>(minOf(bufferedFramesCount, frameCount))
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

    internal val maxIterationCount = when (repeatCount) {
        ENCODED_LOOP_COUNT -> maxOf(0L, codec.repetitionCount + 1L)
        REPEAT_INFINITE -> 0L
        else -> repeatCount.toLong() + 1L
    }

    internal val cumulativeFrameDurationsMillis = LongArray(frameCount).also { durations ->
        var total = 0L
        for (index in durations.indices) {
            // Skia only exposes frame info for multi-frame images.
            val info = if (frameCount > 1) codec.getFrameInfo(index) else null
            requiredFrameIndices[index] = info?.requiredFrame ?: NO_FRAME
            restorePreviousFrames[index] =
                info?.disposalMethod == AnimationDisposalMode.RESTORE_PREVIOUS
            total += info.safeFrameDuration
            durations[index] = total
        }
    }

    private val maxDurationMillis = cumulativeFrameDurationsMillis.last()

    private var invalidateTick by mutableIntStateOf(0)
    private var animationStartTime: TimeMark? = null

    // Register and cancel jobs under frameLock, but start them outside it so an inline dispatcher
    // cannot acquire decodeLock while holding frameLock.
    private var prefetcher: FramePrefetcher? = null
    private var invalidationJob: Job? = null
    private var invalidationFrameIndex = NO_FRAME
    private var invalidationIteration = -1L

    // Allocate native pixel buffers after metadata and state so failures there cannot strand them.
    // The initialization block below owns cleanup if predecoding fails.
    private val bitmapPool = if (bufferedFrames.size > 1 && animatedTransformation == null) {
        FrameBitmapPool(outputImageInfo, bufferedFrames.size)
    } else {
        null
    }
    private val workingBitmaps = try {
        if (bitmapPool == null || decodeImageInfo != outputImageInfo ||
            requiredFrameIndices.any { it != NO_FRAME }
        ) {
            WorkingBitmaps(
                decodeImageInfo = decodeImageInfo,
                outputImageInfo = if (bitmapPool == null) outputImageInfo else decodeImageInfo,
                animatedTransformation = animatedTransformation,
            )
        } else {
            null
        }
    } catch (throwable: Throwable) {
        bitmapPool?.close()
        throw throwable
    }
    override val size = encodedDataSize +
        (frameCount.toLong() * (Int.SIZE_BYTES + 1 + Long.SIZE_BYTES)) +
        (workingBitmaps?.decode?.imageInfo?.byteSize() ?: 0L) +
        (workingBitmaps?.output?.imageInfo?.byteSize() ?: 0L) +
        (bufferedFrames.size * outputImageInfo.byteSize())

    init {
        try {
            for (frameIndex in bufferedFrames.indices) {
                ensureFrame(frameIndex)
            }
        } catch (throwable: Throwable) {
            if (throwable !is Exception || !hasDecodeFailed) {
                closeBitmaps()
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
                    cancelAnimationJobsLocked()
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
                AnimationState.IDLE -> firstBufferedFrameLocked()
                AnimationState.STARTING ->
                    retainedFrame
                    ?: findBufferedFrameLocked(lastDrawnFrameIndex)
                    ?: firstBufferedFrameLocked()
                AnimationState.RUNNING -> {
                    findBufferedFrameLocked(lastDrawnFrameIndex) ?: lastBufferedFrameLocked()
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
            cancelAnimationJobsLocked()
            true
        }
        if (!didStop) return

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
                cancelAnimationJobsLocked()
                true
            }
        }
        if (!shouldClose) return

        coroutineScope.cancel()
        synchronized(decodeLock) {
            try {
                synchronized(frameLock) {
                    closeBitmaps()
                    bufferedFrames.fill(null)
                    lastDrawnFrameIndex = NO_FRAME
                }
            } finally {
                codec.close()
            }
        }
    }

    private fun closeBitmaps() {
        try {
            if (bitmapPool != null) {
                bitmapPool.close()
            } else {
                bufferedFrames.forEach { it?.image?.close() }
            }
        } finally {
            workingBitmaps?.close()
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
        val iteration = elapsedTimeMs / maxDurationMillis
        val isAnimationComplete = maxIterationCount in 1..iteration

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
        prefetchFrames(frameIndex, iteration, startTime)
        scheduleInvalidation(frameIndex, iteration, startTime)
    }

    internal val bufferedFrameCount: Int
        get() = synchronized(frameLock) { bufferedFrames.count { it != null } }

    private val hasDecodeFailed: Boolean
        get() = synchronized(frameLock) { animationState == AnimationState.FAILED }

    private fun frameIndexAt(elapsedTimeMs: Long): Int {
        val iterationElapsedTimeMs = elapsedTimeMs % maxDurationMillis
        var low = 0
        var high = cumulativeFrameDurationsMillis.lastIndex
        while (low < high) {
            val middle = (low + high) ushr 1
            if (iterationElapsedTimeMs < cumulativeFrameDurationsMillis[middle]) {
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
                val frame = findBufferedFrameLocked(lastDrawnFrameIndex)
                    ?: lastBufferedFrameLocked()
                    ?: return@synchronized false
                notifyAnimationEnd = animationState.isRunning
                animationState = AnimationState.FAILED
                retainedFrame = frame
                bufferWindowStart = frame.index
                animationStartTime = null
                cancelAnimationJobsLocked()
                true
            }
        }
        if (didStop && notifyAnimationEnd) onAnimationEnd?.invoke()
    }

    private fun cancelAnimationJobsLocked() {
        prefetcher?.job?.cancel()
        prefetcher = null
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

    private fun prefetchFrames(frameIndex: Int, iteration: Long, startTime: TimeMark) {
        if (bufferedFrames.size <= 1) return

        val count = if (maxIterationCount > 0L && iteration == maxIterationCount - 1L) {
            minOf(bufferedFrames.size, frameCount - frameIndex)
        } else {
            bufferedFrames.size
        }
        val prefetcher = synchronized(frameLock) {
            if (!animationState.isRunning || animationStartTime !== startTime) return
            val window = if (count > 1 && !isBufferWindowCompleteLocked(frameIndex, count)) {
                IntPair(frameIndex, count)
            } else {
                EMPTY_PREFETCH_WINDOW
            }
            if ((this.prefetcher?.window ?: EMPTY_PREFETCH_WINDOW) == window) return
            val prefetcher = this.prefetcher ?: FramePrefetcher().also { this.prefetcher = it }
            prefetcher.window = window
            prefetcher
        }
        prefetcher.wakeups.tryEmit(Unit)
        prefetcher.job.start()
    }

    private fun scheduleInvalidation(frameIndex: Int, iteration: Long, startTime: TimeMark) {
        val job = synchronized(frameLock) {
            if (!animationState.isRunning || animationStartTime !== startTime) return
            if (invalidationFrameIndex == frameIndex &&
                invalidationIteration == iteration &&
                invalidationJob?.isActive == true
            ) {
                return
            }

            invalidationJob?.cancel()
            invalidationFrameIndex = frameIndex
            invalidationIteration = iteration
            // Register the job before dispatching it so a concurrent stop/close cannot miss it.
            coroutineScope.launch(start = CoroutineStart.LAZY) {
                // Decode time and dispatcher delays count toward the frame's duration. If decoding
                // crossed a loop boundary, the displayed frame is already due for replacement.
                val elapsedTimeMs = startTime.elapsedNow().inWholeMilliseconds.coerceAtLeast(0L)
                val delayMs = if (elapsedTimeMs / maxDurationMillis == iteration) {
                    cumulativeFrameDurationsMillis[frameIndex] - elapsedTimeMs % maxDurationMillis
                } else {
                    0L
                }
                delay(delayMs.coerceAtLeast(1L).milliseconds)
                synchronized(frameLock) {
                    if (animationState.isRunning && animationStartTime === startTime) invalidateTick++
                }
            }.also {
                invalidationJob = it
            }
        }
        job.start()
    }

    private fun isBufferWindowCompleteLocked(frameIndex: Int, count: Int): Boolean {
        return bufferedFrames.count {
            it != null && isInBufferWindow(it.index, frameIndex, count)
        } == count
    }

    private fun ensureFrame(frameIndex: Int) {
        try {
            if (!shouldDecodeFrame(frameIndex)) return

            synchronized(decodeLock) {
                val slot = synchronized(frameLock) {
                    if (!shouldDecodeFrameLocked(frameIndex)) return
                    reserveDecodeSlotLocked()
                }

                var image: SkiaImage? = null
                var isRetained = false
                try {
                    val decodedImage = decodeFrame(frameIndex, slot)
                    image = decodedImage
                    val replacedFrame = synchronized(frameLock) {
                        if (animationState.canDecodeFrames &&
                            isInBufferWindow(frameIndex, bufferWindowStart)
                        ) {
                            val replacedFrame = addBufferedFrameLocked(
                                Frame(frameIndex, decodedImage, slot),
                            )
                            isRetained = true
                            replacedFrame
                        } else {
                            null
                        }
                    }
                    if (slot == null) replacedFrame?.image?.close()
                } finally {
                    if (!isRetained) {
                        if (slot == null) {
                            image?.close()
                        } else {
                            synchronized(frameLock) { checkNotNull(bitmapPool).recycle(slot) }
                        }
                    }
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
        return synchronized(frameLock) { shouldDecodeFrameLocked(frameIndex) }
    }

    private fun shouldDecodeFrameLocked(frameIndex: Int): Boolean {
        return animationState.canDecodeFrames &&
            findBufferedFrameLocked(frameIndex) == null &&
            isInBufferWindow(frameIndex, bufferWindowStart)
    }

    private fun reserveDecodeSlotLocked(): FrameBitmapPool.Slot? {
        val pool = bitmapPool ?: return null
        pool.take()?.let { return it }

        // Remove the frame before overwriting its pixels. Keep the last draw available if decoding
        // fails, and prefer evicting frames outside the new prefetch window.
        val index = bufferedFrames.indexOfFirst { frame ->
            frame != null && frame.index != lastDrawnFrameIndex &&
                !isInBufferWindow(frame.index, bufferWindowStart)
        }.takeIf { it >= 0 } ?: bufferedFrames.indexOfFirst { frame ->
            frame != null && frame.index != lastDrawnFrameIndex
        }
        return checkNotNull(removeBufferedFrameLocked(index)?.slot)
    }

    private fun addBufferedFrameLocked(frame: Frame): Frame? {
        val emptyIndex = bufferedFrames.indexOfFirst { it == null }
        if (emptyIndex >= 0) {
            bufferedFrames[emptyIndex] = frame
            return null
        }

        val replacedIndex = bufferedFrames.indexOfFirst { bufferedFrame ->
            bufferedFrame != null &&
                !isInBufferWindow(bufferedFrame.index, bufferWindowStart)
        }.takeIf { it >= 0 } ?: 0
        val replacedFrame = removeBufferedFrameLocked(replacedIndex)
        bufferedFrames[bufferedFrames.lastIndex] = frame
        return replacedFrame
    }

    private fun removeBufferedFrameLocked(index: Int): Frame? {
        val frame = bufferedFrames[index]
        // Preserve decode order so failure fallback can retain the newest valid frame.
        for (slot in index until bufferedFrames.lastIndex) {
            bufferedFrames[slot] = bufferedFrames[slot + 1]
        }
        bufferedFrames[bufferedFrames.lastIndex] = null
        return frame
    }

    private fun decodeFrame(frameIndex: Int, slot: FrameBitmapPool.Slot?): SkiaImage {
        codec.readPixels(
            bitmap = workingBitmaps?.decode ?: checkNotNull(slot).bitmap,
            frame = frameIndex,
            priorFrame = priorFrameFor(frameIndex),
        )
        lastDecodedFrameIndex = frameIndex

        if (slot == null) {
            return SkiaImage.makeFromBitmap(checkNotNull(workingBitmaps).prepareOutput())
        }
        workingBitmaps?.decode?.scalePixelsTo(slot.bitmap)
        slot.bitmap.notifyPixelsChanged()
        return slot.image
    }

    private fun priorFrameFor(frameIndex: Int): Int {
        if (frameIndex <= 0 || lastDecodedFrameIndex !in 0..<frameIndex) return NO_FRAME
        val requiredFrame = requiredFrameIndices[frameIndex]
        if (requiredFrame == NO_FRAME || lastDecodedFrameIndex < requiredFrame) return NO_FRAME
        return if (restorePreviousFrames[lastDecodedFrameIndex]) {
            NO_FRAME
        } else {
            lastDecodedFrameIndex
        }
    }

    private fun isInBufferWindow(
        frameIndex: Int,
        windowStart: Int,
        count: Int = bufferedFrames.size,
    ): Boolean {
        val distance = if (frameIndex >= windowStart) {
            frameIndex.toLong() - windowStart
        } else {
            frameCount.toLong() - windowStart + frameIndex
        }
        return distance < count
    }

    private fun nextFrameIndex(frameIndex: Int, offset: Int): Int {
        return ((frameIndex.toLong() + offset) % frameCount).toInt()
    }

    private fun findBufferedFrameLocked(frameIndex: Int): Frame? {
        return bufferedFrames.firstOrNull { it?.index == frameIndex }
    }

    private fun firstBufferedFrameLocked(): Frame? {
        return bufferedFrames.firstOrNull { it != null }
    }

    private fun lastBufferedFrameLocked(): Frame? {
        return bufferedFrames.lastOrNull { it != null }
    }

    private inner class FramePrefetcher {

        // A wakeup reads the latest window under frameLock, even when draws signal out of order.
        val wakeups = MutableSharedFlow<Unit>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        var window = EMPTY_PREFETCH_WINDOW

        val job = coroutineScope.launch(start = CoroutineStart.LAZY) {
            wakeups.collect {
                val request = synchronized(frameLock) { window }
                val (frameIndex, count) = request
                for (offset in 1 until count) {
                    currentCoroutineContext().ensureActive()
                    if (synchronized(frameLock) { window != request }) return@collect
                    try {
                        ensureFrame(nextFrameIndex(frameIndex, offset))
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        return@collect
                    }
                }
            }
        }
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
        val slot: FrameBitmapPool.Slot?,
    )
}

private const val NO_FRAME = -1
private val EMPTY_PREFETCH_WINDOW = IntPair(NO_FRAME, 0)
