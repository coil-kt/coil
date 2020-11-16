package coil.fetch

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.bitmap.BitmapPool
import coil.decode.Options
import coil.size.PixelSize
import coil.util.copyAssetToFile
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FileFetcherTest {

    private lateinit var context: Context
    private lateinit var pool: BitmapPool

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        pool = BitmapPool(0)
    }

    @Test
    fun basic() {
        val fetcher = FileFetcher(true)
        val file = context.copyAssetToFile("normal.jpg")

        assertTrue(fetcher.handles(file))

        val size = PixelSize(100, 100)
        val result = runBlocking {
            fetcher.fetch(pool, file, size, Options(context))
        }

        assertTrue(result is SourceResult)
        assertEquals("image/jpeg", result.mimeType)
        assertFalse(result.source.exhausted())
    }

    @Test
    fun fileCacheKeyWithLastModified() {
        val fetcher = FileFetcher(true)
        val file = context.copyAssetToFile("normal.jpg")

        file.setLastModified(1234L)
        val firstKey = fetcher.key(file)

        file.setLastModified(4321L)
        val secondKey = fetcher.key(file)

        assertNotEquals(secondKey, firstKey)
    }

    @Test
    fun fileCacheKeyWithoutLastModified() {
        val fetcher = FileFetcher(false)
        val file = context.copyAssetToFile("normal.jpg")

        file.setLastModified(1234L)
        val firstKey = fetcher.key(file)

        file.setLastModified(4321L)
        val secondKey = fetcher.key(file)

        assertEquals(secondKey, firstKey)
    }
}
