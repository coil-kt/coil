package coil.decode

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Build.VERSION.SDK_INT
import androidx.test.core.app.ApplicationProvider
import coil.decode.VideoFrameDecoder.Companion.VIDEO_FRAME_MICROS_KEY
import coil.request.Options
import coil.request.Parameters
import coil.size.Size
import coil.util.assertIsSimilarTo
import coil.util.assumeTrue
import coil.util.decodeBitmapAsset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okio.buffer
import okio.source
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class VideoFrameDecoderTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun noSetFrameTime() = runTest {
        // MediaMetadataRetriever.getFrameAtTime does not work on the emulator pre-API 23.
        assumeTrue(SDK_INT >= 23)

        val result = VideoFrameDecoder(
            source = ImageSource(
                source = context.assets.open("video.mp4").source().buffer(),
                context = context
            ),
            options = Options(context)
        ).decode()

        val actual = (result.drawable as? BitmapDrawable)?.bitmap
        assertNotNull(actual)
        assertFalse(result.isSampled)

        val expected = context.decodeBitmapAsset("video_frame_1.jpg")
        actual.assertIsSimilarTo(expected)
    }

    @Test
    fun specificFrameTime() = runTest {
        // MediaMetadataRetriever.getFrameAtTime does not work on the emulator pre-API 23.
        assumeTrue(SDK_INT >= 23)

        val result = VideoFrameDecoder(
            source = ImageSource(
                source = context.assets.open("video.mp4").source().buffer(),
                context = context
            ),
            options = Options(
                context = context,
                parameters = Parameters.Builder()
                    .set(VIDEO_FRAME_MICROS_KEY, 32600000L)
                    .build()
            )
        ).decode()

        val actual = (result.drawable as? BitmapDrawable)?.bitmap
        assertNotNull(actual)
        assertFalse(result.isSampled)

        val expected = context.decodeBitmapAsset("video_frame_2.jpg")
        actual.assertIsSimilarTo(expected)
    }

    @Test
    fun rotation() = runTest {
        // MediaMetadataRetriever.getFrameAtTime does not work on the emulator pre-API 23.
        assumeTrue(SDK_INT >= 23)

        val result = VideoFrameDecoder(
            source = ImageSource(
                source = context.assets.open("video_rotated.mp4").source().buffer(),
                context = context
            ),
            options = Options(context, size = Size(150, 150))
        ).decode()

        val actual = (result.drawable as? BitmapDrawable)?.bitmap
        assertNotNull(actual)
        assertTrue(result.isSampled)

        val expected = context.decodeBitmapAsset("video_frame_rotated.jpg")
        actual.assertIsSimilarTo(expected, threshold = 0.97)
    }
}
