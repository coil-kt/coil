package coil.fetch

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.bitmappool.BitmapPool
import coil.size.PixelSize
import coil.util.copyAssetToFile
import coil.util.createOptions
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FileFetcherTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var fetcher: FileFetcher
    private lateinit var pool: BitmapPool

    @Before
    fun before() {
        fetcher = FileFetcher()
        pool = BitmapPool(0)
    }

    @Test
    fun basic() {
        val file = context.copyAssetToFile("normal.jpg")

        assertTrue(fetcher.handles(file))

        val size = PixelSize(100, 100)
        val result = runBlocking {
            fetcher.fetch(pool, file, size, createOptions())
        }

        assertTrue(result is SourceResult)
        assertEquals("image/jpeg", result.mimeType)
        assertFalse(result.source.exhausted())
    }

    @Test
    fun fileCacheKeyWithLastModified() {
        val file = context.copyAssetToFile("normal.jpg")

        file.setLastModified(1234L)
        val firstKey = fetcher.key(file)

        file.setLastModified(4321L)
        val secondKey = fetcher.key(file)

        assertNotEquals(secondKey, firstKey)
    }
}
