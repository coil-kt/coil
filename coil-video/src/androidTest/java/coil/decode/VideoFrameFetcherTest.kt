package coil.decode

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import coil.bitmappool.BitmapPool
import coil.fetch.DrawableResult
import coil.fetch.VideoFrameFetcher.Companion.VIDEO_FRAME_MICROS_KEY
import coil.fetch.VideoFrameUriFetcher
import coil.request.Parameters
import coil.size.OriginalSize
import coil.util.createOptions
import coil.util.decodeBitmapAsset
import coil.util.isSimilarTo
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VideoFrameFetcherTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var pool: BitmapPool
    private lateinit var fetcher: VideoFrameUriFetcher

    @Before
    fun before() {
        pool = BitmapPool(0)
        fetcher = VideoFrameUriFetcher(context)
    }

    @Test
    fun noSetFrameTime() {
        val result = runBlocking {
            fetcher.fetch(
                pool = pool,
                data = Uri.parse("file:///android_asset/video.mp4"),
                size = OriginalSize,
                options = createOptions()
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
        val result = runBlocking {
            fetcher.fetch(
                pool = pool,
                data = Uri.parse("file:///android_asset/video.mp4"),
                size = OriginalSize,
                options = createOptions(
                    parameters = Parameters {
                        set(VIDEO_FRAME_MICROS_KEY, 32600000L)
                    }
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
