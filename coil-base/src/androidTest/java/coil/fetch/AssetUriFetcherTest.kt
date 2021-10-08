package coil.fetch

import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.fetch.AssetUriFetcher.Companion.ASSET_FILE_PATH_ROOT
import coil.request.Options
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AssetUriFetcherTest {

    private lateinit var context: Context
    private lateinit var fetcherFactory: AssetUriFetcher.Factory

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        fetcherFactory = AssetUriFetcher.Factory()
    }

    @Test
    fun basic() {
        val uri = "$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/normal.jpg".toUri()
        assertUriFetchesCorrectly(uri)
    }

    @Test
    fun nestedPath() {
        val uri = "$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/exif/large_metadata.jpg".toUri()
        assertUriFetchesCorrectly(uri)
    }

    private fun assertUriFetchesCorrectly(uri: Uri) {
        val result = runBlocking {
            assertNotNull(fetcherFactory.create(uri, Options(context), ImageLoader(context))).fetch()
        }

        assertTrue(result is SourceResult)
        assertEquals("image/jpeg", result.mimeType)
        assertFalse(result.source.source().exhausted())
    }
}
