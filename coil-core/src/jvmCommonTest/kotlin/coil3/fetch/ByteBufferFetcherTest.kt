package coil3.fetch

import java.nio.ByteBuffer
import kotlin.random.Random
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import okio.buffer
import org.junit.Test

class ByteBufferFetcherTest {
    private val factory = ByteBufferFetcher.Factory()

    @Test
    fun testDataIntegrity() = runTest {
        val buffer = ByteBuffer.wrap(DATA.encodeToByteArray())
        val fetched = buffer.asSource().buffer().readUtf8()
        assertEquals(fetched, DATA)
    }

    @Test
    fun testDiscreteRead() = runTest {
        // 64 okio Segments
        val buffer = ByteBuffer.allocate(64 * 8192).slice()
        Random.nextBytes(buffer.array())
        val source = buffer.asSource().buffer()
        val data = source.readByteArray()
        assertContentEquals(buffer.array(), data)
    }
}

private const val DATA = "Hello world!"
