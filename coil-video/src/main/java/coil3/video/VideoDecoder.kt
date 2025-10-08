package coil3.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ColorSpace
import android.graphics.HardwareRenderer
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RenderNode
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.media.MediaDataSource
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import coil3.DrawableImage
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.video.internal.dispatcher
import coil3.video.internal.use
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.yield
import okio.ByteString

/**
 * A [Decoder] that plays back video content as an [Animatable] drawable.
 */
@RequiresApi(23)
@ExperimentalCoilApi
class VideoDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult? {
        val dispatcher = coroutineContext.dispatcher ?: Dispatchers.Default

        return runInterruptible {
            val data = source.use { it.source().readByteString() }

            val metadata = checkNotNull(readMetadata(data)) {
                "Unable to read video metadata."
            }

            val drawable = VideoDrawable(
                videoBytes = data,
                metadata = metadata,
                dispatcher = dispatcher,
            )

            DecodeResult(
                image = drawable.asImage(),
                isSampled = false,
            )
        }
    }

    private fun readMetadata(videoBytes: ByteString): VideoMetadata? {
        return MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(ByteStringMediaDataSource(videoBytes))

            var width = retriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            var height = retriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val durationUs = (retriever.extractMetadata(METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) * 1000L
            val frameRate = retriever.extractMetadata(METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()

            val previewBitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: retriever.frameAtTime

            if (width <= 0 && previewBitmap != null) width = previewBitmap.width
            if (height <= 0 && previewBitmap != null) height = previewBitmap.height

            if (width <= 0 || height <= 0) return@use null

            val safeDurationUs = if (durationUs > 0) durationUs else DEFAULT_DURATION_US
            val safeFrameRate = frameRate?.takeIf { it > 0f } ?: DEFAULT_FRAME_RATE

            val initialFrame = previewBitmap?.let {
                val copy = it.copy(Bitmap.Config.ARGB_8888, false)
                it.recycle()
                copy
            }

            VideoMetadata(
                width = width,
                height = height,
                rotationDegrees = rotation,
                durationUs = safeDurationUs,
                frameRate = safeFrameRate,
                initialFrame = initialFrame,
            )
        }
    }

    class Factory : Decoder.Factory {

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result)) return null
            return VideoDecoder(result.source, options)
        }

        private fun isApplicable(result: SourceFetchResult): Boolean {
            val mimeType = result.mimeType
            if (mimeType != null && mimeType.startsWith("video/")) {
                return true
            }

            val headerIsVideo = result.source.source().use { buffered ->
                runCatching { DecodeUtils.isVideo(buffered) }.getOrDefault(false)
            }
            return headerIsVideo
        }
    }

    private companion object {
        private const val DEFAULT_FRAME_RATE = 30f
        private const val DEFAULT_DURATION_US = 1_000_000L
    }
}

@RequiresApi(23)
private class VideoDrawable(
    private val videoBytes: ByteString,
    private val metadata: VideoMetadata,
    private val dispatcher: CoroutineDispatcher,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : Drawable(), Animatable, DrawableImage.SizeProvider, AutoCloseable {
    private val intrinsicVideoWidth = metadata.displayWidth
    private val intrinsicVideoHeight = metadata.displayHeight
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val drawBounds = Rect()
    private val frameLock = Any()
    private val isReleased = AtomicBoolean(false)
    private val tempMatrix = Matrix()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val frameRenderer: FrameRenderer = createFrameRenderer()
    private var renderJob: Job? = null
    private val invalidateRunnable = Runnable { invalidateSelf() }

    @Volatile
    private var isRunningInternal = false

    @Volatile
    private var currentFrameUs: Long = 0L
    private var pendingFrameUs: Long = 0L
    private var playbackOffsetUs: Long = 0L
    private var startReferenceMark: TimeMark = timeSource.markNow()

    private var retriever: MediaMetadataRetriever? = null
    private var currentMediaDataSource: MediaDataSource? = null

    init {
        metadata.initialFrame?.let { initial ->
            synchronized(frameLock) {
                frameRenderer.renderFrame(initial)
                currentFrameUs = 0L
                pendingFrameUs = 0L
            }
            initial.recycle()
        }
    }

    override fun draw(canvas: Canvas) {
        synchronized(frameLock) {
            drawBounds.set(bounds)
            frameRenderer.draw(canvas, drawBounds, bitmapPaint)
        }
    }

    override fun setAlpha(alpha: Int) {
        bitmapPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        bitmapPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun start() {
        if (isReleased.get() || isRunning) return
        isRunningInternal = true
        startReferenceMark = timeSource.markNow()
        val job = scope.launch { renderLoop() }
        job.invokeOnCompletion {
            if (renderJob === job) {
                renderJob = null
            }
        }
        renderJob = job
    }

    override fun stop() {
        if (!isRunning) return
        isRunningInternal = false
        playbackOffsetUs = if (metadata.durationUs > 0) {
            currentFrameUs % metadata.durationUs
        } else {
            currentFrameUs
        }
        renderJob?.cancel()
        renderJob = null
        unscheduleSelf(invalidateRunnable)
    }

    override fun isRunning(): Boolean = isRunningInternal

    override fun getIntrinsicWidth(): Int = intrinsicVideoWidth

    override fun getIntrinsicHeight(): Int = intrinsicVideoHeight

    override val size: Long
        get() = videoBytes.size.toLong()

    override fun invalidateSelf() {
        if (!isReleased.get()) {
            super.invalidateSelf()
        }
    }

    override fun scheduleSelf(what: Runnable, `when`: Long) {
        if (!isReleased.get()) {
            super.scheduleSelf(what, `when`)
        }
    }

    override fun unscheduleSelf(what: Runnable) {
        if (!isReleased.get()) {
            super.unscheduleSelf(what)
        }
    }

    private fun computeCurrentPositionUs(): Long {
        val elapsedUs = startReferenceMark.elapsedNow().inWholeMicroseconds
        val position = playbackOffsetUs + elapsedUs
        val duration = metadata.durationUs
        return if (duration > 0) position % duration else position
    }

    private suspend fun renderLoop() {
        val frameDelayMs = max(1L, metadata.frameDurationUs / 1000L)
        while (isRunningInternal && !isReleased.get()) {
            coroutineContext.ensureActive()
            val loopStartMark = timeSource.markNow()
            val positionUs = computeCurrentPositionUs()
            pendingFrameUs = positionUs
            renderFrame(positionUs)
            val elapsedMs = loopStartMark.elapsedNow().inWholeMilliseconds
            val delayMs = frameDelayMs - elapsedMs
            if (delayMs > 0) {
                delay(delayMs)
            } else {
                yield()
            }
        }
    }

    private fun renderFrame(timeUs: Long) {
        val retriever = ensureRetriever()
        val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
            ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: retriever.getFrameAtTime(timeUs)
            ?: return

        try {
            synchronized(frameLock) {
                frameRenderer.renderFrame(frame)
                currentFrameUs = pendingFrameUs
            }
        } finally {
            frame.recycle()
        }
        requestInvalidate()
    }

    private fun drawVideoFrame(canvas: Canvas, frame: Bitmap) {
        val frameWidth = frame.width.toFloat()
        val frameHeight = frame.height.toFloat()
        if (frameWidth <= 0f || frameHeight <= 0f) return

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val destWidth = intrinsicVideoWidth.toFloat()
        val destHeight = intrinsicVideoHeight.toFloat()
        val rotation = ((metadata.rotationDegrees % 360) + 360) % 360
        val swapsAxes = rotation % 180 != 0
        val scaleX: Float
        val scaleY: Float
        if (swapsAxes) {
            scaleX = destHeight / frameWidth
            scaleY = destWidth / frameHeight
        } else {
            scaleX = destWidth / frameWidth
            scaleY = destHeight / frameHeight
        }

        tempMatrix.reset()
        tempMatrix.preTranslate(-frameWidth / 2f, -frameHeight / 2f)
        tempMatrix.preScale(scaleX, scaleY)
        tempMatrix.preRotate(rotation.toFloat())
        tempMatrix.preTranslate(destWidth / 2f, destHeight / 2f)

        canvas.drawBitmap(frame, tempMatrix, null)
    }

    private fun ensureRetriever(): MediaMetadataRetriever {
        val existing = retriever
        if (existing != null) return existing
        val newRetriever = MediaMetadataRetriever()
        val mediaDataSource = ByteStringMediaDataSource(videoBytes)
        newRetriever.setDataSource(mediaDataSource)
        retriever = newRetriever
        currentMediaDataSource = mediaDataSource
        return newRetriever
    }

    private fun requestInvalidate() {
        if (isReleased.get()) return
        unscheduleSelf(invalidateRunnable)
        scheduleSelf(invalidateRunnable, 0L)
    }

    override fun close() {
        if (!isReleased.compareAndSet(false, true)) return

        stop()
        scope.cancel()
        renderJob = null
        unscheduleSelf(invalidateRunnable)
        synchronized(frameLock) {
            runCatching { frameRenderer.close() }
        }
        if (SDK_INT >= 29) {
            retriever?.close()
        } else {
            retriever?.release()
        }
        currentMediaDataSource?.close()
        retriever = null
        currentMediaDataSource = null
    }

    private fun createFrameRenderer(): FrameRenderer {
        if (SDK_INT < 29) {
            return SoftwareFrameRenderer(intrinsicVideoWidth, intrinsicVideoHeight, ::drawVideoFrame)
        }
        return runCatching {
            HardwareFrameRenderer(
                width = intrinsicVideoWidth,
                height = intrinsicVideoHeight,
                drawFrame = ::drawVideoFrame,
                timeSource = timeSource,
            )
        }.getOrElse {
            SoftwareFrameRenderer(intrinsicVideoWidth, intrinsicVideoHeight, ::drawVideoFrame)
        }
    }

    private interface FrameRenderer : AutoCloseable {
        fun renderFrame(frame: Bitmap)
        fun draw(canvas: Canvas, bounds: Rect, paint: Paint)
    }

    private class SoftwareFrameRenderer(
        width: Int,
        height: Int,
        private val drawFrame: (Canvas, Bitmap) -> Unit,
    ) : FrameRenderer {
        private val bitmap = createBitmap(width, height)
        private val backingCanvas = Canvas(bitmap)

        override fun renderFrame(frame: Bitmap) {
            drawFrame(backingCanvas, frame)
        }

        override fun draw(canvas: Canvas, bounds: Rect, paint: Paint) {
            if (bounds.isEmpty) {
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            } else {
                canvas.drawBitmap(bitmap, null, bounds, paint)
            }
        }

        override fun close() {
            bitmap.recycle()
        }
    }

    @RequiresApi(29)
    private class HardwareFrameRenderer(
        private val width: Int,
        private val height: Int,
        private val drawFrame: (Canvas, Bitmap) -> Unit,
        timeSource: TimeSource,
    ) : FrameRenderer {
        private val imageReader = ImageReader.newInstance(
            width,
            height,
            PixelFormat.RGBA_8888,
            MAX_IMAGES,
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT,
        )
        private val renderNode = RenderNode("VideoDrawable").apply {
            setPosition(0, 0, width, height)
        }
        private val renderer = HardwareRenderer().apply {
            setSurface(imageReader.surface)
            setContentRoot(renderNode)
            isOpaque = false
            start()
        }
        private val vsyncOriginMark = timeSource.markNow()

        private var currentBitmap: Bitmap? = null

        override fun renderFrame(frame: Bitmap) {
            val recordingCanvas = renderNode.beginRecording(width, height)
            try {
                drawFrame(recordingCanvas, frame)
            } finally {
                renderNode.endRecording()
            }

            renderer.createRenderRequest()
                .setVsyncTime(vsyncOriginMark.elapsedNow().inWholeNanoseconds)
                .syncAndDraw()

            imageReader.acquireLatestImage()?.use { image ->
                val buffer = image.hardwareBuffer ?: return@use
                val colorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
                val hardwareBitmap = runCatching {
                    Bitmap.wrapHardwareBuffer(buffer, colorSpace)
                }.getOrNull()
                buffer.close()
                if (hardwareBitmap != null) {
                    currentBitmap?.recycle()
                    currentBitmap = hardwareBitmap
                }
            }
        }

        override fun draw(canvas: Canvas, bounds: Rect, paint: Paint) {
            val bitmap = currentBitmap ?: return
            if (bounds.isEmpty) {
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            } else {
                canvas.drawBitmap(bitmap, null, bounds, paint)
            }
        }

        override fun close() {
            currentBitmap?.recycle()
            currentBitmap = null
            renderer.stop()
            renderer.destroy()
            imageReader.close()
        }

        private companion object {
            private const val MAX_IMAGES = 2
        }
    }
}

@RequiresApi(23)
private data class VideoMetadata(
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val durationUs: Long,
    val frameRate: Float,
    val initialFrame: Bitmap?,
) {
    val displayWidth: Int
        get() = if (rotationDegrees == 90 || rotationDegrees == 270) height else width

    val displayHeight: Int
        get() = if (rotationDegrees == 90 || rotationDegrees == 270) width else height

    val frameDurationUs: Long
        get() = max(1L, (1_000_000f / frameRate).roundToLong())
}

@RequiresApi(23)
private class ByteStringMediaDataSource(
    private val data: ByteString,
) : MediaDataSource() {

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position < 0L || position > Int.MAX_VALUE.toLong()) return -1

        val startIndex = position.toInt()
        if (startIndex >= data.size) return -1

        val available = data.size - startIndex
        val length = minOf(size, available)
        if (length <= 0) return 0

        var targetIndex = offset
        for (i in 0 until length) {
            buffer[targetIndex++] = data[startIndex + i]
        }
        return length
    }

    override fun getSize() = data.size.toLong()

    override fun close() {}
}
