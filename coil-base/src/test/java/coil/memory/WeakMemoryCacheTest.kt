package coil.memory

import android.content.Context
import androidx.collection.arraySetOf
import androidx.test.core.app.ApplicationProvider
import coil.util.clear
import coil.util.count
import coil.util.createBitmap
import coil.util.getAllocationByteCountCompat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21, 29])
class WeakMemoryCacheTest {

    private lateinit var context: Context
    private lateinit var weakMemoryCache: RealWeakMemoryCache
    private lateinit var references: MutableSet<Any>

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        weakMemoryCache = RealWeakMemoryCache()
        references = arraySetOf()
    }

    @Test
    fun `can retrieve cached value`() {
        val key = "key"
        val bitmap = reference(createBitmap())
        val isSampled = false
        val size = bitmap.getAllocationByteCountCompat()

        weakMemoryCache.set(key, bitmap, isSampled, size)
        val value = weakMemoryCache.get(key)

        assertNotNull(value)
        assertEquals(bitmap, value.bitmap)
        assertEquals(isSampled, value.isSampled)
    }

    @Test
    fun `can hold multiple values`() {
        val bitmap1 = reference(createBitmap())
        weakMemoryCache.set("key1", bitmap1, false, 100)

        val bitmap2 = reference(createBitmap())
        weakMemoryCache.set("key2", bitmap2, false, 100)

        val bitmap3 = reference(createBitmap())
        weakMemoryCache.set("key3", bitmap3, false, 100)

        assertEquals(bitmap1, weakMemoryCache.get("key1")?.bitmap)
        assertEquals(bitmap2, weakMemoryCache.get("key2")?.bitmap)
        assertEquals(bitmap3, weakMemoryCache.get("key3")?.bitmap)

        weakMemoryCache.clear(bitmap2)

        assertEquals(bitmap1, weakMemoryCache.get("key1")?.bitmap)
        assertNull(weakMemoryCache.get("key2"))
        assertEquals(bitmap3, weakMemoryCache.get("key3")?.bitmap)
    }

    @Test
    fun `invalidate removes from cache`() {
        val key = "key"
        val bitmap = reference(createBitmap())

        weakMemoryCache.set(key, bitmap, false, 100)
        weakMemoryCache.invalidate(bitmap)

        assertNull(weakMemoryCache.get(key))
    }

    @Test
    fun `bitmaps with same key are retrieved by size descending`() {
        val bitmap1 = reference(createBitmap())
        val bitmap2 = reference(createBitmap())
        val bitmap3 = reference(createBitmap())
        val bitmap4 = reference(createBitmap())
        val bitmap5 = reference(createBitmap())
        val bitmap6 = reference(createBitmap())
        val bitmap7 = reference(createBitmap())
        val bitmap8 = reference(createBitmap())

        weakMemoryCache.set("key", bitmap1, false, 1)
        weakMemoryCache.set("key", bitmap3, false, 3)
        weakMemoryCache.set("key", bitmap5, false, 5)
        weakMemoryCache.set("key", bitmap7, false, 7)
        weakMemoryCache.set("key", bitmap8, false, 8)
        weakMemoryCache.set("key", bitmap4, false, 4)
        weakMemoryCache.set("key", bitmap6, false, 6)
        weakMemoryCache.set("key", bitmap2, false, 2)

        assertEquals(bitmap8, weakMemoryCache.get("key")?.bitmap)
        weakMemoryCache.invalidate(bitmap8)

        assertEquals(bitmap7, weakMemoryCache.get("key")?.bitmap)
        weakMemoryCache.invalidate(bitmap7)

        assertEquals(bitmap6, weakMemoryCache.get("key")?.bitmap)
        weakMemoryCache.invalidate(bitmap6)

        assertEquals(bitmap5, weakMemoryCache.get("key")?.bitmap)
        weakMemoryCache.invalidate(bitmap5)

        assertEquals(bitmap4, weakMemoryCache.get("key")?.bitmap)
        weakMemoryCache.invalidate(bitmap4)

        assertEquals(bitmap3, weakMemoryCache.get("key")?.bitmap)
        weakMemoryCache.invalidate(bitmap3)

        assertEquals(bitmap2, weakMemoryCache.get("key")?.bitmap)
        weakMemoryCache.invalidate(bitmap2)

        assertEquals(bitmap1, weakMemoryCache.get("key")?.bitmap)
        weakMemoryCache.invalidate(bitmap1)

        // All the values are invalidated.
        assertNull(weakMemoryCache.get("key"))
    }

    @Test
    fun `cleanUp clears all collected values`() {
        val bitmap1 = reference(createBitmap())
        weakMemoryCache.set("key1", bitmap1, false, 100)

        val bitmap2 = reference(createBitmap())
        weakMemoryCache.set("key2", bitmap2, false, 100)

        val bitmap3 = reference(createBitmap())
        weakMemoryCache.set("key3", bitmap3, false, 100)

        weakMemoryCache.clear(bitmap1)
        assertNull(weakMemoryCache.get("key1"))

        weakMemoryCache.clear(bitmap3)
        assertNull(weakMemoryCache.get("key3"))

        assertEquals(3, weakMemoryCache.count())

        weakMemoryCache.cleanUp()

        assertEquals(1, weakMemoryCache.count())

        assertNull(weakMemoryCache.get("key1"))
        assertEquals(bitmap2, weakMemoryCache.get("key2")?.bitmap)
        assertNull(weakMemoryCache.get("key3"))
    }

    /** Hold a strong reference to the value for the duration of the test to prevent it from being garbage collected. */
    private fun <T : Any> reference(value: T): T {
        references.add(value)
        return value
    }
}
