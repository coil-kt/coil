package coil3.gif

import coil3.BitmapImage
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.DecodeResult
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.gif.AnimatedImageDecoderUtils.ENCODED_LOOP_COUNT
import coil3.gif.AnimatedImageDecoderUtils.REPEAT_INFINITE
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
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
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
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Color
import org.jetbrains.skia.Data

@OptIn(ExperimentalCoroutinesApi::class)
class AnimatedSkiaImageDecoderTest {

    private val imageLoader = ImageLoader(context)
    private val imagesToClose = mutableListOf<AnimatedSkiaImage>()

    @AfterTest
    fun tearDown() {
        try {
            imagesToClose.forEach(AnimatedSkiaImage::close)
        } finally {
            imageLoader.shutdown()
        }
    }

    @Test
    fun displaysEveryFrameWithExpectedTimingAcrossIterations() = runTest {
        val timeSource = FakeTimeSource()
        val result = decode("animated_infinite.gif", timeSource)
        val image = assertIs<AnimatedSkiaImage>(result.image)
        assertFalse(image.isRunning())

        repeat(2) {
            for (frame in 1..5) {
                assertFrameIsSimilar(result, frame)
                assertTrue(image.isRunning())
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
    fun singleFrameGifTransformationCanAddTransparency() = runTest {
        val extras = ImageRequest.Builder(context)
            .animatedTransformation { canvas ->
                canvas.clear(Color.TRANSPARENT)
                PixelOpacity.TRANSLUCENT
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
            assertEquals(Color.TRANSPARENT, bitmap.getColor(0, 0))
        }
    }

    @Test
    fun animatedSkiaImageSupportsSingleFrame() = runTest {
        val data = Data.makeFromBytes(STATIC_GIF.toByteArray())
        val codec = data.use { Codec.makeFromData(it) }
        val imageInfo = codec.imageInfo
        val image = AnimatedSkiaImage(
            codec = codec,
            coroutineScope = this,
            timeSource = FakeTimeSource(),
            decodeImageInfo = imageInfo,
            outputImageInfo = imageInfo,
            encodedDataSize = STATIC_GIF.size.toLong(),
            repeatCount = REPEAT_INFINITE,
            bufferedFramesCount = 2,
        )
        imagesToClose += image

        assertEquals(1, image.maxFrameBufferSize)
        assertEquals(1, image.bufferedFrameCount)
        assertFalse(image.isRunning())

        image.start()
        assertFalse(image.isRunning())
        image.toBitmap().use { bitmap ->
            assertEquals(2, bitmap.width)
            assertEquals(2, bitmap.height)
        }
        image.stop()
        assertFalse(image.isRunning())
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
    fun zeroFrameDurationsUseSafeDefault() = runTest {
        val timeSource = FakeTimeSource()
        val result = decode("no_frame_delay.gif", timeSource)
        val image = assertIs<AnimatedSkiaImage>(result.image)

        assertTrue(
            image.cumulativeFrameDurationsMillis.indices.all { index ->
                image.frameDurationMillis(index) == 100L
            },
        )
        assertAdvancesToNextFrame(result, image, timeSource)
    }

    @Test
    fun startsOnFirstDrawAndStartStopControlAnimation() = runTest {
        val timeSource = FakeTimeSource()
        val result = decode("animated_infinite.gif", timeSource)
        val image = assertIs<AnimatedSkiaImage>(result.image)
        assertFalse(image.isRunning())

        assertFrameIsSimilar(result, frame = 1)
        assertTrue(image.isRunning())

        timeSource.advanceBy(400.milliseconds)
        result.image.toBitmap().use { secondFrame ->
            image.start()
            result.image.toBitmap().use { runningFrame ->
                runningFrame.assertIsSimilarTo(secondFrame)
            }

            image.stop()
            assertFalse(image.isRunning())

            timeSource.advanceBy(10.seconds)
            result.image.toBitmap().use { stoppedFrame ->
                stoppedFrame.assertIsSimilarTo(secondFrame)
            }
        }

        image.start()
        assertTrue(image.isRunning())
        timeSource.advanceBy(10.seconds)
        assertFrameIsSimilar(result, frame = 1)
        image.close()
    }

    @Test
    fun encodedRepeatCountFreezesOnLastFrame() = runTest {
        val timeSource = FakeTimeSource()
        val extras = ImageRequest.Builder(context)
            .repeatCount(ENCODED_LOOP_COUNT)
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
    fun startThenStopBeforeFirstDrawOnlyInvokesEndCallback() = runTest {
        var starts = 0
        var ends = 0
        val extras = ImageRequest.Builder(context)
            .onAnimationStart { starts++ }
            .onAnimationEnd { ends++ }
            .build()
            .extras
        val result = decode(
            resource = "animated_infinite.gif",
            timeSource = FakeTimeSource(),
            options = Options(context, extras = extras),
        )
        val image = assertIs<AnimatedSkiaImage>(result.image)

        image.start()
        image.stop()

        assertFalse(image.isRunning())
        assertEquals(0, starts)
        assertEquals(1, ends)
        assertFrameIsSimilar(result, frame = 1)
        assertEquals(0, starts)
        assertEquals(1, ends)
    }

    @Test
    fun stopBeforeFirstDrawPreventsAutomaticStart() = runTest {
        val result = decode("animated_infinite.gif", FakeTimeSource())
        val image = assertIs<AnimatedSkiaImage>(result.image)

        image.stop()
        assertFalse(image.isRunning())
        assertFrameIsSimilar(result, frame = 1)
        assertFalse(image.isRunning())

        image.start()
        assertTrue(image.isRunning())
        assertFrameIsSimilar(result, frame = 1)
        assertTrue(image.isRunning())
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
        val image = assertIs<AnimatedSkiaImage>(result.image)
        assertFalse(image.isRunning())

        assertFrameIsSimilar(result, frame = 1)
        assertTrue(image.isRunning())
        assertEquals(1, starts)
        assertEquals(0, ends)

        image.start()
        assertEquals(1, starts)

        timeSource.advanceBy(2.seconds)
        assertFrameIsSimilar(result, frame = 5)
        assertFalse(image.isRunning())
        assertEquals(1, starts)
        assertEquals(1, ends)

        timeSource.advanceBy(5.seconds)
        assertFrameIsSimilar(result, frame = 5)
        assertEquals(1, starts)
        assertEquals(1, ends)

        image.start()
        assertTrue(image.isRunning())
        assertEquals(1, starts)
        assertFrameIsSimilar(result, frame = 1)
        assertEquals(2, starts)
        image.stop()
        assertFalse(image.isRunning())
        assertEquals(2, ends)
        image.close()
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
        assertEquals(2, image.maxFrameBufferSize)

        repeat(100) {
            result.image.toBitmap().close()
            runCurrent()
            assertTrue(image.bufferedFrameCount <= image.maxFrameBufferSize)
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
    fun restorePreviousDisposalCompositesFramesCorrectly() = runTest {
        val timeSource = FakeTimeSource()
        val result = decode(
            result = disposalGifSource(),
            timeSource = timeSource,
            bufferedFramesCount = 1,
        )

        result.image.toBitmap().use { frame ->
            assertEquals(Color.RED, frame.getColor(0, 0))
            assertEquals(Color.RED, frame.getColor(3, 3))
        }

        timeSource.advanceBy(100.milliseconds)
        result.image.toBitmap().use { frame ->
            assertEquals(Color.GREEN, frame.getColor(0, 0))
            assertEquals(Color.RED, frame.getColor(3, 3))
        }

        timeSource.advanceBy(100.milliseconds)
        result.image.toBitmap().use { frame ->
            assertEquals(Color.RED, frame.getColor(0, 0))
            assertEquals(Color.BLUE, frame.getColor(3, 3))
        }
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
        var ends = 0
        val extras = ImageRequest.Builder(context)
            .onAnimationEnd { ends++ }
            .build()
            .extras
        val result = decode(
            result = corruptFrameGifSource(),
            timeSource = timeSource,
            bufferedFramesCount = 1,
            options = Options(context, extras = extras),
        )
        val image = assertIs<AnimatedSkiaImage>(result.image)
        assertFalse(image.isRunning())

        result.image.toBitmap().use { firstFrame ->
            assertTrue(image.isRunning())
            timeSource.advanceBy(image.frameDurationMillis(0).milliseconds)
            result.image.toBitmap().use { failedFrame ->
                failedFrame.assertIsSimilarTo(firstFrame)
            }
            assertFalse(image.isRunning())
            assertEquals(1, ends)

            timeSource.advanceBy(10.seconds)
            result.image.toBitmap().use { stoppedFrame ->
                stoppedFrame.assertIsSimilarTo(firstFrame)
            }
            assertEquals(1, ends)

            image.start()
            assertFalse(image.isRunning())
        }
        image.close()
    }

    @Test
    fun frameDecodeExceptionWhileBufferingStopsOnLastValidFrame() = runTest {
        val timeSource = FakeTimeSource()
        val result = decode(
            result = corruptFrameGifSource(),
            timeSource = timeSource,
        )
        val image = assertIs<AnimatedSkiaImage>(result.image)
        assertFalse(image.isRunning())
        assertEquals(1, image.bufferedFrameCount)

        result.image.toBitmap().use { firstFrame ->
            timeSource.advanceBy(10.seconds)
            result.image.toBitmap().use { stoppedFrame ->
                stoppedFrame.assertIsSimilarTo(firstFrame)
            }
        }
        image.start()
        assertFalse(image.isRunning())
        image.close()
    }

    @Test
    fun closeIsIdempotentAndPreventsFurtherUse() = runTest {
        val result = decode("animated_infinite.gif", FakeTimeSource())
        val image = assertIs<AnimatedSkiaImage>(result.image)
        assertTrue(image.bufferedFrameCount > 0)

        result.image.toBitmap().close()
        assertTrue(image.isRunning())

        image.close()
        image.close()
        assertFalse(image.isRunning())
        assertEquals(0, image.bufferedFrameCount)

        image.start()
        assertFalse(image.isRunning())
        result.image.toBitmap().close()
        runCurrent()
        assertEquals(0, image.bufferedFrameCount)
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
    fun animatedTransformationCanAddTransparencyToEveryFrame() = runTest {
        val timeSource = FakeTimeSource()
        val extras = ImageRequest.Builder(context)
            .animatedTransformation { canvas ->
                canvas.clear(Color.TRANSPARENT)
                PixelOpacity.TRANSLUCENT
            }
            .build()
            .extras
        val result = decode(
            resource = "animated_infinite.gif",
            timeSource = timeSource,
            options = Options(context, extras = extras),
        )
        val image = assertIs<AnimatedSkiaImage>(result.image)

        repeat(2) {
            result.image.toBitmap().use { bitmap ->
                assertEquals(Color.TRANSPARENT, bitmap.getColor(bitmap.width / 2, bitmap.height / 2))
            }
            timeSource.advanceBy(image.frameDurationMillis(0).milliseconds)
        }
    }

    @Test
    fun factoryOnlyHandlesAnimatedImages() {
        val factory = AnimatedSkiaImageDecoder.Factory()
        val expectedResults = mapOf(
            "animated_infinite.gif" to true,
            "frame1.png" to false,
            "static.webp" to false,
        )

        expectedResults.forEach { (resource, isSupported) ->
            val result = resourceSource(resource)
            result.source.use {
                val decoder = factory.create(result, Options(context), imageLoader)
                assertEquals(isSupported, decoder != null)
            }
        }
    }

    @Test
    fun serviceLoaderFindsDecoder() {
        val decoders = ServiceLoaderComponentRegistry.decoders
        assertTrue(decoders.any { it.factory() is AnimatedSkiaImageDecoder.Factory })
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
                imageLoader = imageLoader,
            ),
        )
        return assertNotNull(decoder.decode()).also { result ->
            (result.image as? AnimatedSkiaImage)?.let(imagesToClose::add)
        }
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
        assertFalse(image.isRunning())
        result.image.toBitmap().use { firstFrame ->
            assertTrue(image.isRunning())
            timeSource.advanceBy(image.frameDurationMillis(0).milliseconds)
            result.image.toBitmap().use { secondFrame ->
                assertFalse(secondFrame.isSimilarTo(firstFrame, threshold = 0.99))
            }
        }
    }

    private fun AnimatedSkiaImage.frameDurationMillis(index: Int): Long {
        val previousDuration = if (index == 0) {
            0L
        } else {
            cumulativeFrameDurationsMillis[index - 1]
        }
        return cumulativeFrameDurationsMillis[index] - previousDuration
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

    private fun disposalGifSource() = Buffer()
        .write(DISPOSAL_GIF)
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

        private val DISPOSAL_GIF = """
            47494638396104000400f00000ff000000000021ff0b4e45545343415045322e300301000000
            21f904040a0000002c0000000004000400000204848f09050021f9040c0a0000002c00000000
            010001008000ff00000000020244010021f904040a0000002c0300030001000100800000ff0000
            0002024401003b
        """.trimIndent().replace("\n", "").decodeHex()
    }
}
