package coil3.decode

import coil3.ImageLoader
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.fakefilesystem.FakeFileSystem

class BlackholeDecoderTest : RobolectricTest() {

    @Test
    fun basic() = runTest {
        val decoderFactory = BlackholeDecoder.Factory()
        val bufferSize = 1024
        val buffer = Buffer().apply { write(ByteArray(bufferSize)) }
        val fetchResult = SourceFetchResult(
            source = ImageSource(
                source = buffer,
                fileSystem = FakeFileSystem(),
            ),
            mimeType = null,
            dataSource = DataSource.MEMORY,
        )
        val decoder = decoderFactory.create(fetchResult, Options(context), ImageLoader(context))

        assertEquals(BlackholeDecoder.Factory.EMPTY_IMAGE, decoder.decode().image)
        assertEquals(bufferSize.toLong(), buffer.size)
    }
}
