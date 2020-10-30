package coil.fetch

import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import coil.bitmap.BitmapPool
import coil.decode.Options
import coil.fetch.AssetUriFetcher.Companion.ASSET_FILE_PATH_ROOT
import coil.size.PixelSize
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AssetUriFetcherTest {

    private lateinit var context: Context
    private lateinit var fetcher: AssetUriFetcher
    private lateinit var pool: BitmapPool

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        fetcher = AssetUriFetcher(context)
        pool = BitmapPool(0)
    }

    @Test
    fun basic() {
        val uri = "$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/normal.jpg".toUri()

        assertTrue(fetcher.handles(uri))
        assertUriFetchesCorrectly(uri)
    }

    @Test
    fun nestedPath() {
        val uri = "$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/exif/large_metadata.jpg".toUri()

        assertTrue(fetcher.handles(uri))
        assertUriFetchesCorrectly(uri)
    }

    private fun assertUriFetchesCorrectly(uri: Uri) {
        val result = runBlocking {
            fetcher.fetch(pool, uri, PixelSize(100, 100), Options(context))
        }

        assertTrue(result is SourceResult)
        assertEquals("image/jpeg", result.mimeType)
        assertFalse(result.source.exhausted())
    }
}
