package coil3.test.utils

import coil3.Bitmap
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.size.Scale
import coil3.size.Size
import coil3.toBitmap
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlinx.coroutines.test.runTest
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem

abstract class AbstractGifDecoderTest {

    protected val testTimeSource = TestTimeSource()

    abstract val decoderFactory: Decoder.Factory

    @Test
    fun `Each frame is displayed correctly with expected timing for one full iteration`() =
        runTest {
            val source = FileSystem.RESOURCES.source("animated_infinite.gif".toPath()).buffer()
            val options = Options(
                context = context,
                size = Size(300, 300),
                scale = Scale.FIT,
            )

            val result = assertNotNull(
                decoderFactory.create(
                    result = source.asSourceResult(),
                    options = options,
                    imageLoader = ImageLoader(context),
                )?.decode(),
            )

            for (frame in 1..5) {
                // Compare each frame of the GIF to the expected bitmap.
                val expected: Bitmap = decodeBitmapResource("frame$frame.png")
                val actual: Bitmap = result.image.toBitmap()
                actual.assertIsSimilarTo(expected)

                // Each frame of the GIF lasts 400ms.
                testTimeSource += 400.milliseconds
            }
        }

    @Test
    fun `First frame is redrawn correctly until second frame is expected`() = runTest {
        val source = FileSystem.RESOURCES.source("animated_infinite.gif".toPath()).buffer()
        val options = Options(
            context = context,
            size = Size(300, 300),
            scale = Scale.FIT,
        )

        val result = assertNotNull(
            decoderFactory.create(
                result = source.asSourceResult(),
                options = options,
                imageLoader = ImageLoader(context),
            )?.decode(),
        )

        // Each frame of the GIF lasts 400ms.
        // Check that the first frame is drawn during 400ms.
        for (i in 1..4) {
            val expected: Bitmap = decodeBitmapResource("frame1.png")
            val actual: Bitmap = result.image.toBitmap()
            actual.assertIsSimilarTo(expected)

            testTimeSource += 100.milliseconds
        }

        // Once the 400ms have passed, the second frame should be displayed.
        val expected: Bitmap = decodeBitmapResource("frame2.png")
        val actual: Bitmap = result.image.toBitmap()
        actual.assertIsSimilarTo(expected)
    }

    @Test
    fun `Each frame is displayed correctly with expected timing for two full iterations`() =
        runTest {
            val source = FileSystem.RESOURCES.source("animated_infinite.gif".toPath()).buffer()
            val options = Options(
                context = context,
                size = Size(300, 300),
                scale = Scale.FIT,
            )

            val result = assertNotNull(
                decoderFactory.create(
                    result = source.asSourceResult(),
                    options = options,
                    imageLoader = ImageLoader(context),
                )?.decode(),
            )

            // First iteration
            for (frame in 1..5) {
                // Compare each frame of the GIF to the expected bitmap.
                val expected: Bitmap = decodeBitmapResource("frame$frame.png")
                val actual: Bitmap = result.image.toBitmap()
                actual.assertIsSimilarTo(expected)

                // Each frame of the GIF lasts 400ms.
                testTimeSource += 400.milliseconds
            }

            // Second iteration
            for (frame in 1..5) {
                // Compare each frame of the GIF to the expected bitmap.
                val expected: Bitmap = decodeBitmapResource("frame$frame.png")
                val actual: Bitmap = result.image.toBitmap()
                actual.assertIsSimilarTo(expected)

                // Each frame of the GIF lasts 400ms.
                testTimeSource += 400.milliseconds
            }
        }

    @Test
    fun `Image with repeat count of 3 is played 3 times then freezes on last frame`() =
        runTest {
            val source = FileSystem.RESOURCES.source("animated_3loops.gif".toPath()).buffer()
            val options = Options(
                context = context,
                size = Size(300, 300),
                scale = Scale.FIT,
            )

            val result = assertNotNull(
                decoderFactory.create(
                    result = source.asSourceResult(),
                    options = options,
                    imageLoader = ImageLoader(context),
                )?.decode(),
            )

            for (loop in 1..3) {
                for (frame in 1..5) {
                    // Compare each frame of the GIF to the expected bitmap.
                    val expected: Bitmap = decodeBitmapResource("frame$frame.png")
                    val actual: Bitmap = result.image.toBitmap()
                    actual.assertIsSimilarTo(expected)

                    // Each frame of the GIF lasts 400ms.
                    testTimeSource += 400.milliseconds
                }
            }

            val intervals = arrayOf(
                400.milliseconds,
                400.milliseconds,
                400.milliseconds,
                1.seconds,
                3.seconds,
                5.seconds,
            )

            // The loop is done; no matter how much time passes, the last frame should be displayed.
            for (interval in intervals) {
                val expected: Bitmap = decodeBitmapResource("frame5.png")
                val actual: Bitmap = result.image.toBitmap()
                actual.assertIsSimilarTo(expected)

                testTimeSource += interval
            }
        }

    fun BufferedSource.asSourceResult(
        mimeType: String? = null,
        dataSource: DataSource = DataSource.DISK,
    ) = SourceFetchResult(
        source = ImageSource(this, FakeFileSystem()),
        mimeType = mimeType,
        dataSource = dataSource,
    )
}
