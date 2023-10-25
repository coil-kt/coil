package coil.fetch

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import androidx.test.core.app.ApplicationProvider
import coil.FileMediaDataSource
import coil.ImageLoader
import coil.request.Options
import coil.util.assumeTrue
import coil.util.copyAssetToFile
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class MediaDataSourceFetcherTest {

    private lateinit var context: Context
    private lateinit var fetcherFactory: MediaDataSourceFetcher.Factory

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        fetcherFactory = MediaDataSourceFetcher.Factory()
    }

    @Test
    fun basic() = runTest {
        assumeTrue(SDK_INT >= 23)

        val file = context.copyAssetToFile("video.mp4")

        val dataSource = FileMediaDataSource(file)
        val fetcher = assertIs<MediaDataSourceFetcher>(
            fetcherFactory.create(dataSource, Options(context), ImageLoader(context)),
        )

        val result = fetcher.fetch()

        assertIs<SourceResult>(result)
        assertEquals(null, result.mimeType)
        assertIs<MediaDataSourceFetcher.MediaSourceMetadata>(result.source.metadata)
    }
}
