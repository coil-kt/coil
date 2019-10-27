package coil.fetch

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import coil.bitmappool.BitmapPool
import coil.size.PixelSize
import coil.util.createOptions
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AssetUriFetcherTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var fetcher: AssetUriFetcher
    private lateinit var pool: BitmapPool

    @Before
    fun before() {
        fetcher = AssetUriFetcher(context)
        pool = BitmapPool(0)
    }

    @Test
    fun basic() {
        val uri = Uri.parse("file:///android_asset/normal.jpg")

        assertTrue(fetcher.handles(uri))
        assertUriFetchesCorrectly(uri)
    }

    @Test
    fun nestedPath() {
        val uri = Uri.parse("file:///android_asset/exif/large_metadata.jpg")

        assertTrue(fetcher.handles(uri))
        assertUriFetchesCorrectly(uri)
    }

    private fun assertUriFetchesCorrectly(uri: Uri) {
        val result = runBlocking {
            fetcher.fetch(pool, uri, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is SourceResult)
        assertEquals("image/jpeg", result.mimeType)
        assertFalse(result.source.exhausted())
    }
}
