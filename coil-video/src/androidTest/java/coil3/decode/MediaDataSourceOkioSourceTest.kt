package coil3.decode

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import androidx.test.core.app.ApplicationProvider
import coil3.FileMediaDataSource
import coil3.fetch.MediaDataSourceFetcher
import coil3.test.assumeTrue
import coil3.test.copyAssetToFile
import kotlinx.coroutines.test.runTest
import okio.buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test

class MediaDataSourceOkioSourceTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun mediaDataSourceOkioSource() = runTest {
        assumeTrue(SDK_INT >= 23)
        val file = context.copyAssetToFile("video_frame_1.jpg")

        val expected = file.readBytes()
        val source = MediaDataSourceFetcher.MediaDataSourceOkioSource(FileMediaDataSource(file))
        val actual = source.buffer().readByteArray()

        assertArrayEquals(expected, actual)
    }
}
