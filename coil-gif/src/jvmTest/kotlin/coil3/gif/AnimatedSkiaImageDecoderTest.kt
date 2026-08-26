package coil3.gif

import coil3.BitmapImage
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.DecodeResult
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import coil3.test.utils.FakeTimeSource
import coil3.test.utils.assertIsSimilarTo
import coil3.test.utils.context
import coil3.test.utils.decodeBitmapResource
import coil3.test.utils.isSimilarTo
import coil3.toBitmap
import coil3.util.ServiceLoaderComponentRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.BufferedSource
import okio.ByteString.Companion.decodeHex
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import org.jetbrains.skia.Color

@OptIn(ExperimentalCoroutinesApi::class)
class AnimatedSkiaImageDecoderTest {

    @Test
    fun displaysEveryFrameWithExpectedTimingAcrossIterations() = runTest {
        val timeSource = FakeTimeSource()
        val result = decode("animated_infinite.gif", timeSource)

        repeat(2) {
            for (frame in 1..5) {
                assertFrameIsSimilar(result, frame)
                timeSource.advanceBy(400.milliseconds)
                runCurrent()
            }
        }
    }

    @Test
    fun animatedWebPIsDecodedAndAdvancesFrames() = runTest {
        val timeSource = FakeTimeSource()
        val result = decode(
            resource = "animated.webp",
            timeSource = timeSource,
            options = Options(
                context = context,
                size = Size(100, 100),
                precision = Precision.EXACT,
            ),
        )
        val image = assertIs<AnimatedSkiaImage>(result.image)
        assertEquals(100, image.width)
        assertEquals(100, image.height)
        assertTrue(result.isSampled)
        assertAdvancesToNextFrame(result, image, timeSource)
    }

    @Test
    fun singleFrameGifDecodesToBitmapImage() = runTest {
        val result = decode(
            result = staticGifSource(),
            timeSource = FakeTimeSource(),
        )

        val image = assertIs<BitmapImage>(result.image)
        image.bitmap.use {
            assertEquals(2, image.width)
            assertEquals(2, image.height)
            assertFalse(result.isSampled)
        }
    }

    @Test
    fun singleFrameGifIsScaledAndReportsSampling() = runTest {
        val result = decode(
            result = staticGifSource(),
            timeSource = FakeTimeSource(),
            options = Options(
                context = context,
                size = Size(1, 1),
                precision = Precision.EXACT,
            ),
        )

        val image = assertIs<BitmapImage>(result.image)
        image.bitmap.use {
            assertEquals(1, image.width)
            assertEquals(1, image.height)
            assertTrue(result.isSampled)
        }
    }

    @Test
    fun singleFrameGifAppliesAnimatedTransformation() = runTest {
        val extras = ImageRequest.Builder(context)
            .animatedTransformation { canvas ->
                canvas.clear(Color.RED)
                PixelOpacity.OPAQUE
            }
            .build()
            .extras
        val result = decode(
            result = staticGifSource(),
            timeSource = FakeTimeSource(),
            options = Options(context, extras = extras),
        )

        val image = assertIs<BitmapImage>(result.image)
        image.bitmap.use { bitmap ->
            assertEquals(Color.RED, bitmap.getColor(0, 0))
        }
    }

    @Test
    fun redrawsCurrentFrameUntilItsDurationElapses() = runTest {
        val timeSource = FakeTimeSource()
        val result = decode("animated_infinite.gif", timeSource)

        repeat(4) {
            assertFrameIsSimilar(result, frame = 1)
            timeSource.advanceBy(100.milliseconds)
        }
        assertFrameIsSimilar(result, frame = 2)
    }

    @Test
    fun encodedRepeatCountFreezesOnLastFrame() = runTest {
        val timeSource = FakeTimeSource()
        val extras = ImageRequest.Builder(context)
            .repeatCount(AnimatedSkiaImageDecoder.ENCODED_LOOP_COUNT)
            .build()
            .extras
        val result = decode(
            resource = "animated_3loops.gif",
            timeSource = timeSource,
            options = Options(context, extras = extras),
        )
        val image = assertIs<AnimatedSkiaImage>(result.image)
        assertEquals(3, image.maxIterationCount)

        repeat(3) {
            for (frame in 1..5) {
                assertFrameIsSimilar(result, frame)
                timeSource.advanceBy(400.milliseconds)
                runCurrent()
            }
        }

        listOf(400.milliseconds, 1.seconds, 3.seconds, 5.seconds).forEach { interval ->
            assertFrameIsSimilar(result, frame = 5)
            timeSource.advanceBy(interval)
        }
    }

    @Test
    fun explicitRepeatCountInvokesCallbacksOnce() = runTest {
        val timeSource = FakeTimeSource()
        var starts = 0
        var ends = 0
        val extras = ImageRequest.Builder(context)
            .repeatCount(0)
            .onAnimationStart { starts++ }
            .onAnimationEnd { ends++ }
            .build()
            .extras
        val result = decode(
            resource = "animated_infinite.gif",
            timeSource = timeSource,
            options = Options(context, extras = extras),
        )

        assertFrameIsSimilar(result, frame = 1)
        assertEquals(1, starts)
        assertEquals(0, ends)

        timeSource.advanceBy(2.seconds)
        assertFrameIsSimilar(result, frame = 5)
        assertEquals(1, starts)
        assertEquals(1, ends)

        timeSource.advanceBy(5.seconds)
        assertFrameIsSimilar(result, frame = 5)
        assertEquals(1, starts)
        assertEquals(1, ends)
    }

    @Test
    fun frameBufferRemainsBoundedAcrossManyIterations() = runTest {
        val timeSource = FakeTimeSource()
        val result = decode(
            resource = "animated_infinite.gif",
            timeSource = timeSource,
            bufferedFramesCount = 2,
        )
        val image = assertIs<AnimatedSkiaImage>(result.image)
        assertEquals(2, image.frameBufferCapacity)

        repeat(100) {
            result.image.toBitmap().close()
            runCurrent()
            assertTrue(image.bufferedFrameCount <= image.frameBufferCapacity)
            timeSource.advanceBy(400.milliseconds)
        }
    }

    @Test
    fun missingFrameIsDecodedBeforeDrawReturns() = runTest {
        val timeSource = FakeTimeSource()
        val result = decode(
            resource = "animated_infinite.gif",
            timeSource = timeSource,
            bufferedFramesCount = 1,
        )

        assertFrameIsSimilar(result, frame = 1)
        timeSource.advanceBy(800.milliseconds)
        assertFrameIsSimilar(result, frame = 3)
    }

    @Test
    fun exactSizeScalesFramesAndReportsSampling() = runTest {
        val timeSource = FakeTimeSource()
        val result = decode(
            resource = "animated_infinite.gif",
            timeSource = timeSource,
            options = Options(
                context = context,
                size = Size(150, 100),
                scale = Scale.FIT,
                precision = Precision.EXACT,
            ),
        )

        assertEquals(100, result.image.width)
        assertEquals(100, result.image.height)
        assertTrue(result.isSampled)

        val image = assertIs<AnimatedSkiaImage>(result.image)
        assertAdvancesToNextFrame(result, image, timeSource)
    }

    @Test
    fun exactSizeUpscalesFrames() = runTest {
        val timeSource = FakeTimeSource()
        val result = decode(
            resource = "animated_infinite.gif",
            timeSource = timeSource,
            options = Options(
                context = context,
                size = Size(600, 600),
                precision = Precision.EXACT,
            ),
        )

        val image = assertIs<AnimatedSkiaImage>(result.image)
        assertEquals(600, image.width)
        assertEquals(600, image.height)
        assertFalse(result.isSampled)
        assertAdvancesToNextFrame(result, image, timeSource)
    }

    @Test
    fun inexactSizeDoesNotUpscaleFrames() = runTest {
        val result = decode(
            resource = "animated_infinite.gif",
            timeSource = FakeTimeSource(),
            options = Options(
                context = context,
                size = Size(600, 600),
                scale = Scale.FIT,
                precision = Precision.INEXACT,
            ),
        )

        assertEquals(300, result.image.width)
        assertEquals(300, result.image.height)
        assertFalse(result.isSampled)
    }

    @Test
    fun maxBitmapSizeBoundsFrames() = runTest {
        val extras = ImageRequest.Builder(context)
            .maxBitmapSize(Size(120, 80))
            .build()
            .extras
        val result = decode(
            resource = "animated_infinite.gif",
            timeSource = FakeTimeSource(),
            options = Options(context, extras = extras),
        )

        assertEquals(80, result.image.width)
        assertEquals(80, result.image.height)
        assertTrue(result.isSampled)
    }

    @Test
    fun largeSourceDimensionsUseBoundedDecodeBitmap() = runTest {
        val extras = ImageRequest.Builder(context)
            .maxBitmapSize(Size(32, 32))
            .build()
            .extras
        val result = decode(
            result = largeCanvasGifSource(),
            timeSource = FakeTimeSource(),
            options = Options(context, extras = extras),
        )

        val image = assertIs<AnimatedSkiaImage>(result.image)
        assertEquals(32, image.width)
        assertEquals(32, image.height)
        assertTrue(result.isSampled)
        assertTrue(image.size < LARGE_CANVAS_MINIMUM_BITMAP_SIZE)
    }

    @Test
    fun frameDecodeExceptionStopsOnLastValidFrame() = runTest {
        val timeSource = FakeTimeSource()
        val result = decode(
            result = corruptFrameGifSource(),
            timeSource = timeSource,
            bufferedFramesCount = 1,
        )
        val image = assertIs<AnimatedSkiaImage>(result.image)
        assertFalse(image.isAnimationStopped)

        result.image.toBitmap().use { firstFrame ->
            timeSource.advanceBy(image.frameDurationsMs.first().milliseconds)
            result.image.toBitmap().use { failedFrame ->
                failedFrame.assertIsSimilarTo(firstFrame)
            }
            assertTrue(image.isAnimationStopped)

            timeSource.advanceBy(10.seconds)
            result.image.toBitmap().use { stoppedFrame ->
                stoppedFrame.assertIsSimilarTo(firstFrame)
            }
        }
    }

    @Test
    fun frameDecodeExceptionWhileBufferingStopsOnLastValidFrame() = runTest {
        val timeSource = FakeTimeSource()
        val result = decode(
            result = corruptFrameGifSource(),
            timeSource = timeSource,
        )
        val image = assertIs<AnimatedSkiaImage>(result.image)
        assertTrue(image.isAnimationStopped)
        assertEquals(1, image.bufferedFrameCount)

        result.image.toBitmap().use { firstFrame ->
            timeSource.advanceBy(10.seconds)
            result.image.toBitmap().use { stoppedFrame ->
                stoppedFrame.assertIsSimilarTo(firstFrame)
            }
        }
    }

    @Test
    fun animatedTransformationIsAppliedToFramePixels() = runTest {
        val extras = ImageRequest.Builder(context)
            .animatedTransformation { canvas ->
                canvas.clear(Color.RED)
                PixelOpacity.OPAQUE
            }
            .build()
            .extras
        val result = decode(
            resource = "animated_infinite.gif",
            timeSource = FakeTimeSource(),
            options = Options(context, extras = extras),
        )

        result.image.toBitmap().use { bitmap ->
            assertEquals(Color.RED, bitmap.getColor(bitmap.width / 2, bitmap.height / 2))
        }
    }

    @Test
    fun factoryOnlyHandlesAnimatedImages() {
        val factory = AnimatedSkiaImageDecoder.Factory()
        val imageLoader = ImageLoader(context)

        val gifSource = resourceSource("animated_infinite.gif")
        assertNotNull(factory.create(gifSource, Options(context), imageLoader))

        val pngSource = resourceSource("frame1.png")
        assertNull(factory.create(pngSource, Options(context), imageLoader))

        val staticWebPSource = resourceSource("static.webp")
        assertNull(factory.create(staticWebPSource, Options(context), imageLoader))
    }

    @Test
    fun serviceLoaderFindsDecoder() {
        val decoders = ServiceLoaderComponentRegistry.decoders
        assertTrue(decoders.any { it.factory() is AnimatedSkiaImageDecoder.Factory })
    }

    @Test
    fun factoryRejectsInvalidBufferSize() {
        assertFailsWith<IllegalArgumentException> {
            AnimatedSkiaImageDecoder.Factory(bufferedFramesCount = 0)
        }
    }

    @Test
    fun repeatCountRejectsValuesBelowEncodedLoopCount() {
        ImageRequest.Builder(context)
            .repeatCount(AnimatedSkiaImageDecoder.ENCODED_LOOP_COUNT)

        assertFailsWith<IllegalArgumentException> {
            ImageRequest.Builder(context).repeatCount(-3)
        }
    }

    private suspend fun decode(
        resource: String,
        timeSource: FakeTimeSource,
        bufferedFramesCount: Int = AnimatedSkiaImageDecoder.Factory.DEFAULT_BUFFERED_FRAMES_COUNT,
        options: Options = Options(context),
    ): DecodeResult {
        return decode(
            result = resourceSource(resource),
            timeSource = timeSource,
            bufferedFramesCount = bufferedFramesCount,
            options = options,
        )
    }

    private suspend fun decode(
        result: SourceFetchResult,
        timeSource: FakeTimeSource,
        bufferedFramesCount: Int = AnimatedSkiaImageDecoder.Factory.DEFAULT_BUFFERED_FRAMES_COUNT,
        options: Options = Options(context),
    ): DecodeResult {
        val factory = AnimatedSkiaImageDecoder.Factory(
            bufferedFramesCount = bufferedFramesCount,
            timeSource = timeSource,
        )
        val decoder = assertNotNull(
            factory.create(
                result = result,
                options = options,
                imageLoader = ImageLoader(context),
            ),
        )
        return assertNotNull(decoder.decode())
    }

    private suspend fun assertFrameIsSimilar(result: DecodeResult, frame: Int) {
        decodeBitmapResource("frame$frame.png").use { expected ->
            result.image.toBitmap().use { actual ->
                actual.assertIsSimilarTo(expected)
            }
        }
    }

    private suspend fun assertAdvancesToNextFrame(
        result: DecodeResult,
        image: AnimatedSkiaImage,
        timeSource: FakeTimeSource,
    ) {
        result.image.toBitmap().use { firstFrame ->
            timeSource.advanceBy(image.frameDurationsMs.first().milliseconds)
            result.image.toBitmap().use { secondFrame ->
                assertFalse(secondFrame.isSimilarTo(firstFrame, threshold = 0.99))
            }
        }
    }

    private fun resourceSource(resource: String): SourceFetchResult {
        return FileSystem.RESOURCES.source(resource.toPath()).buffer().asSourceResult()
    }

    private fun BufferedSource.asSourceResult() = SourceFetchResult(
        source = ImageSource(this, FakeFileSystem()),
        mimeType = null,
        dataSource = DataSource.DISK,
    )

    private fun staticGifSource() = Buffer()
        .write(STATIC_GIF)
        .asSourceResult()

    private fun largeCanvasGifSource() = Buffer()
        .write(LARGE_CANVAS_GIF)
        .asSourceResult()

    private fun corruptFrameGifSource() = Buffer()
        .write(CORRUPT_FRAME_GIF)
        .asSourceResult()

    companion object {
        private const val LARGE_CANVAS_SIZE = 1_024
        private const val LARGE_CANVAS_MINIMUM_BITMAP_SIZE =
            LARGE_CANVAS_SIZE.toLong() * LARGE_CANVAS_SIZE

        private val STATIC_GIF = """
            47494638396102000200800000000000ffffff21f90401000000002c000000000100010000
            02024401003b
        """.trimIndent().replace("\n", "").decodeHex()

        private val LARGE_CANVAS_GIF = """
            47494638396100040004800000000000ffffff21f90401010000002c000000000100010000
            020244010021f90401010000002c00000000010001000002024c01003b
        """.trimIndent().replace("\n", "").decodeHex()

        private val CORRUPT_FRAME_GIF = """
            47494638396102000200800000000000ffffff21f90401010000002c000000000100010000
            020244010021f90401010000002c0000000001000100000202ffff003b
        """.trimIndent().replace("\n", "").decodeHex()
    }
}
