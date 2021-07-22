package coil.decode

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Build.VERSION.SDK_INT
import androidx.test.core.app.ApplicationProvider
import coil.decode.VideoFrameDecoder.Companion.VIDEO_FRAME_MICROS_KEY
import coil.request.Options
import coil.request.Parameters
import coil.size.PixelSize
import coil.util.decodeBitmapAsset
import coil.util.isSimilarTo
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.source
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VideoFrameDecoderTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun noSetFrameTime() {
        // MediaMetadataRetriever.getFrameAtTime does not work on the emulator pre-API 23.
        assumeTrue(SDK_INT >= 23)

        val result = runBlocking {
            VideoFrameDecoder(
                source = ImageSource(
                    source = context.assets.open("video.mp4").source().buffer(),
                    context = context
                ),
                options = Options(context)
            ).decode()
        }

        val actual = (result.drawable as? BitmapDrawable)?.bitmap
        assertNotNull(actual)
        assertFalse(result.isSampled)

        val expected = context.decodeBitmapAsset("video_frame_1.jpg")
        assertTrue(actual.isSimilarTo(expected))
    }

    @Test
    fun specificFrameTime() {
        // MediaMetadataRetriever.getFrameAtTime does not work on the emulator pre-API 23.
        assumeTrue(SDK_INT >= 23)

        val result = runBlocking {
            VideoFrameDecoder(
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
        }

        val actual = (result.drawable as? BitmapDrawable)?.bitmap
        assertNotNull(actual)
        assertFalse(result.isSampled)

        val expected = context.decodeBitmapAsset("video_frame_2.jpg")
        assertTrue(actual.isSimilarTo(expected))
    }

    @Test
    fun rotation() {
        // MediaMetadataRetriever.getFrameAtTime does not work on the emulator pre-API 23.
        assumeTrue(SDK_INT >= 23)

        val result = runBlocking {
            VideoFrameDecoder(
                source = ImageSource(
                    source = context.assets.open("video_rotated.mp4").source().buffer(),
                    context = context
                ),
                options = Options(context, size = PixelSize(150, 150))
            ).decode()
        }

        val actual = (result.drawable as? BitmapDrawable)?.bitmap
        assertNotNull(actual)
        assertTrue(result.isSampled)

        val expected = context.decodeBitmapAsset("video_frame_rotated.jpg")
        assertTrue(actual.isSimilarTo(expected, threshold = 0.97))
    }
}
