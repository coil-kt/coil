@file:Suppress("EXPERIMENTAL_API_USAGE")

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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UriFetcherTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var loader: UriFetcher
    private lateinit var pool: BitmapPool

    @Before
    fun before() {
        loader = UriFetcher(context)
        pool = FakeBitmapPool()
    }

    @Test
    fun basicAssetLoad() {
        val uri = Uri.parse("file:///android_asset/normal.jpg")
        assertTrue(loader.handles(uri))
        assertEquals(uri.toString(), loader.key(uri))

        val result = runBlocking {
            loader.fetch(pool, uri, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is SourceResult)
        assertFalse(result.source.exhausted())
    }

    @Test
    fun basicExtractFileName() {
        val uri = Uri.parse("file:///android_asset/something.jpg")
        val result = loader.extractAssetFileName(uri)
        assertEquals("something.jpg", result)
    }

    @Test
    fun emptyExtractFileName() {
        val uri = Uri.parse("file:///android_asset/")
        val result = loader.extractAssetFileName(uri)
        assertEquals(null, result)
    }

    @Test
    fun nonAssetUriExtractFileName() {
        val uri = Uri.parse("file:///fake/file/path")
        val result = loader.extractAssetFileName(uri)
        assertEquals(null, result)
    }
}
