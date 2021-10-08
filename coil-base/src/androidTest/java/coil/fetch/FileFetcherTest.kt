package coil.fetch

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.request.Options
import coil.size.PixelSize
import coil.util.copyAssetToFile
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FileFetcherTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun basic() {
        val file = context.copyAssetToFile("normal.jpg")
        val options = Options(context, size = PixelSize(100, 100))
        val fetcher = FileFetcher.Factory().create(file, options, ImageLoader(context))

        assertNotNull(fetcher)

        val result = runBlocking { fetcher.fetch() }

        assertTrue(result is SourceResult)
        assertEquals("image/jpeg", result.mimeType)
        assertNotNull(result.source.file())
    }
}
