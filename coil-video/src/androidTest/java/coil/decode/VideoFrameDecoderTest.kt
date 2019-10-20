package coil.decode

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import coil.bitmappool.BitmapPool
import coil.decode.VideoFrameDecoder.Companion.VIDEO_FRAME_MICROS_KEY
import coil.request.Parameters
import coil.size.OriginalSize
import coil.util.createOptions
import coil.util.decodeBitmapAsset
import coil.util.isSimilarTo
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.source
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VideoFrameDecoderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var pool: BitmapPool
    private lateinit var decoder: VideoFrameDecoder

    @Before
    fun before() {
        pool = BitmapPool(0)
        decoder = VideoFrameDecoder(context)
    }

    @Test
    fun noSetFrameTime() {
        val result = runBlocking {
            decoder.decode(
                pool = pool,
                source = context.assets.open("video.mp4").source().buffer(),
                size = OriginalSize,
                options = createOptions()
            )
        }

        val actual = (result.drawable as? BitmapDrawable)?.bitmap
        assertNotNull(actual)
        assertFalse(result.isSampled)

        val expected = context.decodeBitmapAsset("video_frame_1.jpg")
        assertTrue(actual.isSimilarTo(expected))
    }

    @Test
    fun specificFrameTime() {
        val result = runBlocking {
            decoder.decode(
                pool = pool,
                source = context.assets.open("video.mp4").source().buffer(),
                size = OriginalSize,
                options = createOptions(
                    parameters = Parameters {
                        set(VIDEO_FRAME_MICROS_KEY, 32600000L)
                    }
                )
            )
        }

        val actual = (result.drawable as? BitmapDrawable)?.bitmap
        assertNotNull(actual)
        assertFalse(result.isSampled)

        val expected = context.decodeBitmapAsset("video_frame_2.jpg")
        assertTrue(actual.isSimilarTo(expected))
    }
}
