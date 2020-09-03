package coil.fetch

import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Build.VERSION.SDK_INT
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import coil.bitmap.BitmapPool
import coil.fetch.VideoFrameFetcher.Companion.ASSET_FILE_PATH_ROOT
import coil.fetch.VideoFrameFetcher.Companion.VIDEO_FRAME_MICROS_KEY
import coil.request.Parameters
import coil.size.OriginalSize
import coil.util.createOptions
import coil.util.decodeBitmapAsset
import coil.util.isSimilarTo
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VideoFrameFetcherTest {

    private lateinit var context: Context
    private lateinit var pool: BitmapPool
    private lateinit var fetcher: VideoFrameUriFetcher

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        pool = BitmapPool(0)
        fetcher = VideoFrameUriFetcher(context)
    }

    @Test
    fun noSetFrameTime() {
        // MediaMetadataRetriever.getFrameAtTime does not work on the emulator pre-API 23.
        assumeTrue(SDK_INT >= 23)

        val result = runBlocking {
            fetcher.fetch(
                pool = pool,
                data = "$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/video.mp4".toUri(),
                size = OriginalSize,
                options = createOptions(context)
            )
        }

        assertTrue(result is DrawableResult)
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
            fetcher.fetch(
                pool = pool,
                data = "$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/video.mp4".toUri(),
                size = OriginalSize,
                options = createOptions(
                    context = context,
                    parameters = Parameters.Builder()
                        .set(VIDEO_FRAME_MICROS_KEY, 32600000L)
                        .build()
                )
            )
        }

        assertTrue(result is DrawableResult)
        val actual = (result.drawable as? BitmapDrawable)?.bitmap
        assertNotNull(actual)
        assertFalse(result.isSampled)

        val expected = context.decodeBitmapAsset("video_frame_2.jpg")
        assertTrue(actual.isSimilarTo(expected))
    }
}
