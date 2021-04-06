package coil.fetch

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.test.core.app.ApplicationProvider
import coil.bitmap.BitmapPool
import coil.decode.Options
import coil.decode.VideoFrameDecoder
import coil.size.OriginalSize
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
    private lateinit var pool: BitmapPool
    private lateinit var decoder: VideoFrameDecoder

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        pool = BitmapPool(0)
        decoder = VideoFrameDecoder(context)
    }

    @Test
    fun basic() {
        // MediaMetadataRetriever.getFrameAtTime does not work on the emulator pre-API 23.
        assumeTrue(SDK_INT >= Build.VERSION_CODES.M)

        val result = runBlocking {
            decoder.decode(
                pool = pool,
                source = context.assets.open("video.mp4").source().buffer(),
                size = OriginalSize,
                options = Options(context)
            )
        }

        val actual = (result.drawable as? BitmapDrawable)?.bitmap
        assertNotNull(actual)
        assertFalse(result.isSampled)

        val expected = context.decodeBitmapAsset("video_frame_1.jpg")
        assertTrue(actual.isSimilarTo(expected))
    }
}
