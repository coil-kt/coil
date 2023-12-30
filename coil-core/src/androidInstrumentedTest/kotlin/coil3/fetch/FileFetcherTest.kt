package coil3.fetch

import coil3.ImageLoader
import coil3.request.Options
import coil3.size.Size
import coil3.test.utils.context
import coil3.test.utils.copyAssetToFile
import coil3.toUri
import coil3.util.SCHEME_FILE
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FileFetcherTest {

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
}
