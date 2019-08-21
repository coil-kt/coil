package coil.memory

import coil.bitmappool.RealBitmapPool
import coil.util.DEFAULT_BITMAP_SIZE
import coil.util.createBitmap
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class MemoryCacheTest {

    private lateinit var counter: BitmapReferenceCounter

    @Before
    fun before() {
        counter = BitmapReferenceCounter(RealBitmapPool(0))
    }

    @Test
    fun `can retrieve cached value`() {
        val cache = MemoryCache(counter, (2 * DEFAULT_BITMAP_SIZE).toInt())

        val bitmap = createBitmap()
        cache.set("1", bitmap, false)

        assertEquals(bitmap, cache["1"]?.bitmap)
    }

    @Test
    fun `least recently used value is evicted`() {
        val cache = MemoryCache(counter, (2 * DEFAULT_BITMAP_SIZE).toInt())

        val first = createBitmap()
        cache.set("1", first, false)

        val second = createBitmap()
        cache.set("2", second, false)

        val third = createBitmap()
        cache.set("3", third, false)

        assertNull(cache["1"])
    }

    @Test
    fun `value is gone after clearMemory(key) is called`() {
        val cache = MemoryCache(counter, (2 * DEFAULT_BITMAP_SIZE).toInt())
        val bitmap = createBitmap()
        cache.set("1", bitmap, false)
        cache.clearMemory("1")
        assertNull(cache["1"])
    }
}
