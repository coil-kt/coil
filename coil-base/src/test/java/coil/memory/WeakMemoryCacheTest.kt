package coil.memory

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.util.createBitmap
import coil.util.getAllocationByteCountCompat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class WeakMemoryCacheTest {

    private lateinit var context: Context
    private lateinit var weakMemoryCache: WeakMemoryCache
    private lateinit var references: MutableSet<Any?>

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        weakMemoryCache = WeakMemoryCache()
        references = mutableSetOf()
    }

    @Test
    fun `can retrieve cached value`() {
        val key = "test"
        val bitmap = reference(createBitmap())
        val isSampled = false
        val size = bitmap.getAllocationByteCountCompat()

        weakMemoryCache.set(key, bitmap, isSampled, size)
        val value = weakMemoryCache.get(key)

        assertNotNull(value)
        assertEquals(bitmap, value.bitmap)
        assertEquals(isSampled, value.isSampled)
    }

    /** Hold a hard reference to the value for the duration of the test. */
    private fun <T> reference(value: T): T {
        references.add(value)
        return value
    }
}
