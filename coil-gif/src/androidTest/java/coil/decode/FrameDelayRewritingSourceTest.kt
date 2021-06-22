package coil.decode

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import okio.Buffer
import okio.Source
import okio.buffer
import okio.source
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FrameDelayRewritingSourceTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun doesNotRewriteGifWithValidFrameDelay() {
        val expected = context.assets.open("animated.gif").source().readBuffer()
        val actual = FrameDelayRewritingSource(context.assets.open("animated.gif").source()).readBuffer()

        assertEquals(expected, actual)
    }

    @Test
    fun onlyRewritesFrameDelay() {
        val expected = context.assets.open("no_frame_delay.gif").source().readByteArray()
        val actual = FrameDelayRewritingSource(context.assets.open("no_frame_delay.gif").source()).readByteArray()

        val graphicsControlExtensionIndexes = arrayOf(32, 40, 11_880, 11_888, 22_443, 22_451, 32_624, 32_632, 43_637,
            43_645, 54_275, 54_283, 65_070, 65_078, 75_062, 75_070, 86_526, 86_534, 98_134, 98_142)

        graphicsControlExtensionIndexes.forEach { index ->
            val frameDelayIndex = index + 4
            assertEquals(10, actual[frameDelayIndex])
            actual[frameDelayIndex] = 0 // Reset the frame delay to the original value.
        }

        assertTrue(expected.contentEquals(actual))
    }

    private fun Source.readBuffer() = Buffer().also { it.writeAll(this) }

    private fun Source.readByteArray() = buffer().readByteArray()
}
