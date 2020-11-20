package coil.decode

import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.util.createTestMainDispatcher
import coil.util.runBlockingTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.Source
import okio.Timeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class InterruptibleSourceTest {

    private lateinit var mainDispatcher: TestCoroutineDispatcher

    @Before
    fun before() {
        mainDispatcher = createTestMainDispatcher()
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun `thread is not interrupted while reading`() = runBlockingTest {
        val isReading = AtomicBoolean(false)
        val source = object : Source {
            override fun read(sink: Buffer, byteCount: Long): Long {
                isReading.set(true)
                Thread.sleep(100)
                isReading.set(false)
                return byteCount
            }
            override fun timeout() = Timeout.NONE
            override fun close() {}
        }
        val buffer = Buffer()

        try {
            // Need to cancel from another thread.
            withContext(Dispatchers.IO) {
                delay(50)
                cancel()
            }

            withInterruptibleSource(source) { interruptibleSource ->
                assertFailsWith<InterruptedException> {
                    // Read until interrupted.
                    while (true) assertEquals(1024, interruptibleSource.read(buffer, 1024))
                }
            }
        } catch (e: Exception) {
            assertTrue(e is CancellationException)
            assertFalse(isReading.get()) // Ensure we weren't interrupted while reading from the source.
            assertFalse(Thread.interrupted()) // Ensure the interrupted state doesn't leak.
        }
    }
}
