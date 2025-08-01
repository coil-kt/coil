package coil3.video

import android.os.Build.VERSION.SDK_INT
import coil3.Extras
import coil3.decode.AssetMetadata
import coil3.decode.ImageSource
import coil3.request.Options
import coil3.size.Size
import coil3.test.utils.assertIsSimilarTo
import coil3.test.utils.assumeTrue
import coil3.test.utils.bitmap
import coil3.test.utils.context
import coil3.test.utils.copyAssetToFile
import coil3.test.utils.decodeBitmapAsset
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import okio.source
import org.junit.Test

class VideoFrameDecoderTest {

    @Test
    fun noSetFrameTime() = runTest {
        // MediaMetadataRetriever does not work on the emulator pre-API 23.
        assumeTrue(SDK_INT >= 23)

        val result = VideoFrameDecoder(
            source = ImageSource(
                source = context.assets.open("video.mp4").source().buffer(),
                fileSystem = FileSystem.SYSTEM,
            ),
            options = Options(context),
        ).decode()

        val actual = result.image.bitmap
        assertNotNull(actual)
        assertFalse(result.isSampled)

        val expected = context.decodeBitmapAsset("video_frame_1.jpg")
        actual.assertIsSimilarTo(expected)
    }

    @Test
    fun noSetFrameTime_FakeFileSystem() = runTest {
        // MediaMetadataRetriever does not work on the emulator pre-API 23.
        assumeTrue(SDK_INT >= 23)

        val fileSystem = FakeFileSystem()
        val path = fileSystem.workingDirectory / "video.mp4"

        fileSystem.write(path) {
            writeAll(context.assets.open("video.mp4").source().buffer())
        }

        val result = VideoFrameDecoder(
            source = ImageSource(
                file = path,
                fileSystem = fileSystem,
            ),
            options = Options(context),
        ).decode()

        val actual = result.image.bitmap
        assertNotNull(actual)
        assertFalse(result.isSampled)

        val expected = context.decodeBitmapAsset("video_frame_1.jpg")
        actual.assertIsSimilarTo(expected)
    }

    @Test
    fun specificFrameTime() = runTest {
        // MediaMetadataRetriever does not work on the emulator pre-API 23.
        assumeTrue(SDK_INT >= 23)

        val result = VideoFrameDecoder(
            source = ImageSource(
                source = context.assets.open("video.mp4").source().buffer(),
                fileSystem = FileSystem.SYSTEM,
            ),
            options = Options(
                context = context,
                extras = Extras.Builder()
                    .set(Extras.Key.videoFrameMicros, 32600000L)
                    .build(),
            ),
        ).decode()

        val actual = result.image.bitmap
        assertNotNull(actual)
        assertFalse(result.isSampled)

        val expected = context.decodeBitmapAsset("video_frame_2.jpg")
        actual.assertIsSimilarTo(expected)
    }

    @Test
    fun specificFramePercent() = runTest {
        // MediaMetadataRetriever does not work on the emulator pre-API 23.
        assumeTrue(SDK_INT >= 23)

        val result = VideoFrameDecoder(
            source = ImageSource(
                source = context.assets.open("video.mp4").source().buffer(),
                fileSystem = FileSystem.SYSTEM,
            ),
            options = Options(
                context = context,
                extras = Extras.Builder()
                    .set(Extras.Key.videoFramePercent, 0.525)
                    .build(),
            ),
        ).decode()

        val actual = result.image.bitmap
        assertNotNull(actual)
        assertFalse(result.isSampled)

        val expected = context.decodeBitmapAsset("video_frame_2.jpg")
        actual.assertIsSimilarTo(expected)
    }

    @Test
    fun specificFrameIndex() = runTest {
        // MediaMetadataRetriever#getFrameAtIndex does not work on the emulator pre-API 28.
        assumeTrue(SDK_INT >= 28)

        val result = VideoFrameDecoder(
            source = ImageSource(
                source = context.assets.open("video.mp4").source().buffer(),
                fileSystem = FileSystem.SYSTEM,
            ),
            options = Options(
                context = context,
                extras = Extras.Builder()
                    .set(Extras.Key.videoFrameIndex, 807)
                    .build(),
            ),
        ).decode()

        val actual = result.image.bitmap
        assertNotNull(actual)
        assertFalse(result.isSampled)

        val expected = context.decodeBitmapAsset("video_frame_2.jpg")
        actual.assertIsSimilarTo(expected)
    }

    @Test
    fun rotation() = runTest {
        // MediaMetadataRetriever does not work on the emulator pre-API 23.
        assumeTrue(SDK_INT >= 23)

        val result = VideoFrameDecoder(
            source = ImageSource(
                source = context.assets.open("video_rotated.mp4").source().buffer(),
                fileSystem = FileSystem.SYSTEM,
            ),
            options = Options(context, size = Size(150, 150)),
        ).decode()

        val actual = result.image.bitmap
        assertNotNull(actual)
        assertTrue(result.isSampled)

        val expected = context.decodeBitmapAsset("video_frame_rotated.jpg")
        actual.assertIsSimilarTo(expected, threshold = 0.97)
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1482 */
    @Test
    fun nestedAsset() = runTest {
        // MediaMetadataRetriever does not work on the emulator pre-API 23.
        assumeTrue(SDK_INT >= 23)

        val path = "nested/video.mp4"
        val result = VideoFrameDecoder(
            source = ImageSource(
                source = context.assets.open(path).source().buffer(),
                fileSystem = FileSystem.SYSTEM,
                metadata = AssetMetadata(path),
            ),
            options = Options(context),
        ).decode()

        val actual = result.image.bitmap
        assertNotNull(actual)
        assertFalse(result.isSampled)

        val expected = context.decodeBitmapAsset("video_frame_1.jpg")
        actual.assertIsSimilarTo(expected)
    }

    @Test
    fun mediaDataSource() = runTest {
        // MediaMetadataRetriever does not work on the emulator pre-API 23.
        assumeTrue(SDK_INT >= 23)
        val file = context.copyAssetToFile("video.mp4")

        val dataSource = FileMediaDataSource(file)
        val result = VideoFrameDecoder(
            source = ImageSource(
                source = MediaDataSourceFetcher.MediaDataSourceOkioSource(dataSource).buffer(),
                fileSystem = FileSystem.SYSTEM,
                metadata = MediaDataSourceFetcher.MediaSourceMetadata(dataSource),
            ),
            options = Options(context),
        ).decode()

        val actual = result.image.bitmap
        assertNotNull(actual)
        assertFalse(result.isSampled)

        val expected = context.decodeBitmapAsset("video_frame_1.jpg")
        actual.assertIsSimilarTo(expected)
    }

    @Test
    fun embeddedThumbnailPresentAndEmbeddedThumbnailOptionEnabled() = runTest {
        // MediaMetadataRetriever does not work on the emulator pre-API 23.
        assumeTrue(SDK_INT >= 23)
        val result = VideoFrameDecoder(
            source = ImageSource(
                source = context.assets.open("video_with_thumb.mp4").source().buffer(),
                fileSystem = FileSystem.SYSTEM,
            ),
            options = Options(
                context = context,
                extras = Extras.Builder()
                    .set(Extras.Key.preferVideoFrameEmbeddedThumbnail, true)
                    .build(),
            ),
        ).decode()

        val actual = result.image.bitmap
        assertNotNull(actual)
        assertFalse(result.isSampled)

        val expected = context.decodeBitmapAsset("video_with_thumb.jpg")
        actual.assertIsSimilarTo(expected)
    }

    @Test
    fun embeddedThumbnailPresentAndEmbeddedThumbnailOptionDisabled() = runTest {
        // MediaMetadataRetriever does not work on the emulator pre-API 23.
        assumeTrue(SDK_INT >= 23)
        val result = VideoFrameDecoder(
            source = ImageSource(
                source = context.assets.open("video_with_thumb.mp4").source().buffer(),
                fileSystem = FileSystem.SYSTEM,
            ),
            options = Options(
                context = context,
                extras = Extras.Builder()
                    .set(Extras.Key.videoFramePercent, 0.2582)
                    .set(Extras.Key.preferVideoFrameEmbeddedThumbnail, false)
                    .build(),
            ),
        ).decode()

        val actual = result.image.bitmap
        assertNotNull(actual)
        assertFalse(result.isSampled)

        val expected = context.decodeBitmapAsset("video_with_thumb_frame_402.jpg")
        actual.assertIsSimilarTo(expected)
    }

    @Test
    fun embeddedThumbnailNotPresentAndEmbeddedThumbnailOptionEnabled() = runTest {
        // MediaMetadataRetriever does not work on the emulator pre-API 23.
        assumeTrue(SDK_INT >= 23)
        val result = VideoFrameDecoder(
            source = ImageSource(
                source = context.assets.open("video.mp4").source().buffer(),
                fileSystem = FileSystem.SYSTEM,
            ),
            options = Options(
                context = context,
                extras = Extras.Builder()
                    .set(Extras.Key.videoFramePercent, 0.525)
                    .set(Extras.Key.preferVideoFrameEmbeddedThumbnail, true)
                    .build(),
            ),
        ).decode()

        val actual = result.image.bitmap
        assertNotNull(actual)
        assertFalse(result.isSampled)

        val expected = context.decodeBitmapAsset("video_frame_2.jpg")
        actual.assertIsSimilarTo(expected)
    }

    @Test
    fun embeddedThumbnailNotPresentAndEmbeddedThumbnailOptionDisabled() = runTest {
        // MediaMetadataRetriever does not work on the emulator pre-API 23.
        assumeTrue(SDK_INT >= 23)
        val result = VideoFrameDecoder(
            source = ImageSource(
                source = context.assets.open("video.mp4").source().buffer(),
                fileSystem = FileSystem.SYSTEM,
            ),
            options = Options(
                context = context,
                extras = Extras.Builder()
                    .set(Extras.Key.videoFramePercent, 0.525)
                    .set(Extras.Key.preferVideoFrameEmbeddedThumbnail, false)
                    .build(),
            ),
        ).decode()

        val actual = result.image.bitmap
        assertNotNull(actual)
        assertFalse(result.isSampled)

        val expected = context.decodeBitmapAsset("video_frame_2.jpg")
        actual.assertIsSimilarTo(expected)
    }
}
