package coil3.fetch

import coil3.ImageLoader
import coil3.request.Options
import coil3.size.Size
import coil3.test.utils.context
import coil3.test.utils.copyAssetToFile
import coil3.toUri
import coil3.util.ASSET_FILE_PATH_ROOT
import coil3.util.SCHEME_FILE
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.Test

class FileUriFetcherTest {
    private val fetcherFactory = FileUriFetcher.Factory()

    @Test
    fun basic() = runTest {
        val file = "$SCHEME_FILE://${context.copyAssetToFile("normal.jpg")}".toUri()
        val options = Options(context, size = Size(100, 100))
        val fetcher = fetcherFactory.create(file, options, ImageLoader(context))

        assertNotNull(fetcher)

        val result = fetcher.fetch()

        assertIs<SourceFetchResult>(result)
        assertEquals("image/jpeg", result.mimeType)
        assertNotNull(result.source.file())
    }

    @Test
    fun doesNotHandleAssetUris() {
        val uri = "$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/asset.jpg".toUri()
        assertNull(fetcherFactory.create(uri, Options(context), ImageLoader(context)))
    }

    @Test
    fun doesNotHandleGenericString() {
        val uri = "generic_string".toUri()
        assertNull(fetcherFactory.create(uri, Options(context), ImageLoader(context)))
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1513 */
    @Test
    fun ignoresAfterPathCorrectly() = runTest {
        val uri = "$SCHEME_FILE:///sdcard/file.jpg?query=value&query2=value#fragment".toUri()
        val fetcher = assertNotNull(fetcherFactory.create(uri, Options(context), ImageLoader(context)))
        val result = assertIs<SourceFetchResult>(fetcher.fetch())
        assertEquals("/sdcard/file.jpg".toPath(), result.source.file())
    }
}
