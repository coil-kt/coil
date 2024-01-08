package coil3.gif

import java.io.ByteArrayInputStream
import kotlin.random.Random
import kotlin.test.assertContentEquals
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.buffer
import okio.source
import org.junit.Test

class FallbackSourceTest {
    @Test
    fun testBuffer() = runTest {
        val bytes = Random.nextBytes(64 * 1024)
        val buffer = Buffer().apply { write(bytes) }
        val byteBuffer = buffer.squashToDirectByteBuffer()
        val cnt = byteBuffer.remaining()
        val bytesNow = ByteArray(cnt).apply { byteBuffer.get(this) }
        assertContentEquals(bytes, bytesNow)
    }

    @Test
    fun testBufferedSource() = runTest {
        val bytes = Random.nextBytes(64 * 1024)
        val buffer = ByteArrayInputStream(bytes).source().buffer()
        val byteBuffer = buffer.squashToDirectByteBuffer()
        val cnt = byteBuffer.remaining()
        val bytesNow = ByteArray(cnt).apply { byteBuffer.get(this) }
        assertContentEquals(bytes, bytesNow)
    }
}
