package coil.fetch

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.bitmappool.BitmapPool
import coil.bitmappool.FakeBitmapPool
import coil.size.PixelSize
import coil.util.createOptions
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.sink
import okio.source
import org.junit.Before
import org.junit.Test
import java.io.File
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
        pool = FakeBitmapPool()
    }

    @Test
    fun basic() {
        // Copy the asset to filesDir.
        val source = context.assets.open("normal.jpg").source().buffer()
        val file = File(context.filesDir.absolutePath + File.separator + "normal.jpg")
        val sink = file.sink().buffer()
        sink.writeAll(source)

        assertTrue(fetcher.handles(file))

        val result = runBlocking {
            fetcher.fetch(pool, file, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is SourceResult)
        assertEquals("image/jpeg", result.mimeType)
        assertFalse(result.source.exhausted())
    }

    @Test
    fun fileCacheKeyWithLastModified() {
        val file = File(context.filesDir.absolutePath + File.separator + "file.jpg")

        // Copy the asset to filesDir.
        val source = context.assets.open("normal.jpg").source().buffer()
        val sink = file.sink().buffer()
        sink.writeAll(source)

        file.setLastModified(1234L)
        val firstKey = fetcher.key(file)

        file.setLastModified(4321L)
        val secondKey = fetcher.key(file)

        assertNotEquals(secondKey, firstKey)
    }
}
