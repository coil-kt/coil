package coil.fetch

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import coil.bitmappool.BitmapPool
import coil.bitmappool.FakeBitmapPool
import coil.size.PixelSize
import coil.util.createOptions
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AssetUriFetcherTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var fetcher: AssetUriFetcher
    private lateinit var pool: BitmapPool

    @Before
    fun before() {
        fetcher = AssetUriFetcher(context)
        pool = FakeBitmapPool()
    }

    @Test
    fun basic() {
        val uri = Uri.parse("file:///android_asset/normal.jpg")
        assertTrue(fetcher.handles(uri))

        val result = runBlocking {
            fetcher.fetch(pool, uri, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is SourceResult)
        assertFalse(result.source.exhausted())
    }
}
