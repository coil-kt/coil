package coil3.video

import android.os.Build.VERSION.SDK_INT
import coil3.test.utils.assumeTrue
import coil3.test.utils.context
import coil3.test.utils.copyAssetToFile
import kotlinx.coroutines.test.runTest
import okio.buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class MediaDataSourceOkioSourceTest {

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
