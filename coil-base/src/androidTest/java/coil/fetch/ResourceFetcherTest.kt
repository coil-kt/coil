package coil.fetch

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import coil.base.test.R
import coil.bitmappool.FakeBitmapPool
import coil.decode.DrawableDecoderService
import coil.size.PixelSize
import coil.util.createOptions
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResourceFetcherTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var pool: FakeBitmapPool
    private lateinit var drawableDecoder: DrawableDecoderService
    private lateinit var fetcher: ResourceFetcher

    @Before
    fun before() {
        pool = FakeBitmapPool()
        drawableDecoder = DrawableDecoderService(context, pool)
        fetcher = ResourceFetcher(context, drawableDecoder)
    }

    @Test
    fun rasterImage() {
        val resId = R.drawable.normal

        assertTrue(fetcher.handles(resId))

        val result = runBlocking {
            fetcher.fetch(pool, resId, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is SourceResult)
        assertEquals("image/jpeg", result.mimeType)
        assertFalse(result.source.exhausted())
    }

    @Test
    fun vectorDrawable() {
        val resId = R.drawable.ic_android

        assertTrue(fetcher.handles(resId))

        val result = runBlocking {
            fetcher.fetch(pool, resId, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is DrawableResult)
        assertTrue(result.drawable is BitmapDrawable)
    }
}
