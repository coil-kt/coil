package coil3.fetch

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import androidx.test.core.app.ApplicationProvider
import coil3.FileMediaDataSource
import coil3.ImageLoader
import coil3.request.Options
import coil3.util.assumeTrue
import coil3.util.copyAssetToFile
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
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

        assertIs<SourceFetchResult>(result)
        assertNull(result.mimeType)
        assertIs<MediaDataSourceFetcher.MediaSourceMetadata>(result.source.metadata)
    }
}
