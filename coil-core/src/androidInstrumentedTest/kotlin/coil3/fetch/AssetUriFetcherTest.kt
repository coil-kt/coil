package coil3.fetch

import android.content.ContentResolver.SCHEME_FILE
import coil3.ImageLoader
import coil3.Uri
import coil3.request.Options
import coil3.test.utils.context
import coil3.toUri
import coil3.util.ASSET_FILE_PATH_ROOT
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AssetUriFetcherTest {

    private val fetcherFactory = AssetUriFetcher.Factory()

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

        assertIs<SourceFetchResult>(result)
        assertEquals("image/jpeg", result.mimeType)
        assertFalse(result.source.source().exhausted())
    }
}
