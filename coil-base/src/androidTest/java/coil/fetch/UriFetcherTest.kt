@file:Suppress("EXPERIMENTAL_API_USAGE")

package coil.fetch

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
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
    fun basicFileFetch() {
        // Copy the asset to filesDir.
        val source = context.assets.open("normal.jpg").source().buffer()
        val file = File(context.filesDir.absolutePath + File.separator + "normal.jpg")
        val sink = file.sink().buffer()
        sink.writeAll(source)

        val uri = file.toUri()
        assertTrue(loader.handles(uri))

        val result = runBlocking {
            loader.fetch(pool, uri, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is SourceResult)
        assertFalse(result.source.exhausted())
    }

    @Test
    fun basicAssetFetch() {
        val uri = Uri.parse("file:///android_asset/normal.jpg")
        assertTrue(loader.handles(uri))

        val result = runBlocking {
            loader.fetch(pool, uri, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is SourceResult)
        assertFalse(result.source.exhausted())
    }

    @Test
    fun basicExtractPath() {
        val uri = Uri.parse("file:///android_asset/something.jpg")
        val result = loader.extractAssetPath(uri)
        assertEquals("something.jpg", result)
    }

    @Test
    fun nestedDirectoriesExtractPath() {
        val uri = Uri.parse("file:///android_asset/img/foo/bar/test.jpg")
        val result = loader.extractAssetPath(uri)
        assertEquals("img/foo/bar/test.jpg", result)
    }

    @Test
    fun emptyExtractPath() {
        val uri = Uri.parse("file:///android_asset/")
        val result = loader.extractAssetPath(uri)
        assertEquals(null, result)
    }

    @Test
    fun nonAssetUriExtractPath() {
        val uri = Uri.parse("file:///fake/file/path")
        val result = loader.extractAssetPath(uri)
        assertEquals(null, result)
    }

    @Test
    fun fileCacheKeyWithLastModified() {
        val file = File(context.filesDir.absolutePath + File.separator + "file.jpg")
        val uri = file.toUri()

        // Copy the asset to filesDir.
        val source = context.assets.open("normal.jpg").source().buffer()
        val sink = file.sink().buffer()
        sink.writeAll(source)

        file.setLastModified(1234L)
        val firstKey = loader.key(uri)

        file.setLastModified(4321L)
        val secondKey = loader.key(uri)

        assertNotEquals(secondKey, firstKey)
    }

    @Test
    fun assetCacheKeyWithLastModified() {
        val uri = Uri.parse("file:///android_asset/normal.jpg")
        assertEquals("$uri:0", loader.key(uri))
    }

    @Test
    fun nonfileCacheKeyEqualsUri() {
        val uri = Uri.parse("content://fake/content/path")
        assertEquals(uri.toString(), loader.key(uri))
    }
}
