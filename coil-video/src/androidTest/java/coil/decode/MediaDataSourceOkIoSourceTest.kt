package coil.decode

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.FileMediaDataSource
import coil.fetch.MediaDataSourceFetcher
import coil.util.copyAssetToFile
import kotlinx.coroutines.test.runTest
import okio.buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test

class MediaDataSourceOkIoSourceTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun mediaDataSourceOkIoSource() = runTest {
        val file = context.copyAssetToFile("video_frame_1.jpg")

        val expected = file.readBytes()
        val source = MediaDataSourceFetcher.MediaDataSourceOkIoSource(FileMediaDataSource(file))
        val actual = source.buffer().readByteArray()

        assertArrayEquals(expected, actual)
    }
}
