package coil.fetch

import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.request.Options
import coil.util.ASSET_FILE_PATH_ROOT
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AssetUriFetcherTest {

    private lateinit var context: Context
    private lateinit var fetcherFactory: AssetUriFetcher.Factory

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        fetcherFactory = AssetUriFetcher.Factory()
    }

    @Test
    fun basic() = runTest {
        val uri = "$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/normal.jpg".toUri()
        assertUriFetchesCorrectly(uri)
    }

    @Test
    fun nestedPath() = runTest {
        val uri = "$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/exif/large_metadata.jpg".toUri()
        assertUriFetchesCorrectly(uri)
    }

    private suspend fun assertUriFetchesCorrectly(uri: Uri) {
        val result = assertNotNull(
            fetcherFactory.create(
                data = uri,
                options = Options(context),
                imageLoader = ImageLoader(context)
            )
        ).fetch()

        assertTrue(result is SourceResult)
        assertEquals("image/jpeg", result.mimeType)
        assertFalse(result.source.source().exhausted())
    }
}
