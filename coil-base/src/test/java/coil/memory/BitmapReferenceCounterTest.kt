package coil.memory

import coil.bitmappool.BitmapPool
import coil.memory.StrongMemoryCache.Key
import coil.util.DEFAULT_BITMAP_SIZE
import coil.util.count
import coil.util.createBitmap
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BitmapReferenceCounterTest {

    private lateinit var weakMemoryCache: WeakMemoryCache
    private lateinit var pool: BitmapPool
    private lateinit var counter: BitmapReferenceCounter

    @Before
    fun before() {
        weakMemoryCache = RealWeakMemoryCache()
        pool = BitmapPool(DEFAULT_BITMAP_SIZE)
        counter = BitmapReferenceCounter(weakMemoryCache, pool, null)
    }

    @Test
    fun `count is incremented`() {
        val bitmap = createBitmap()
        counter.increment(bitmap)

        assertEquals(1, counter.count(bitmap))
    }

    @Test
    fun `count is decremented`() {
        val bitmap = createBitmap()
        counter.increment(bitmap)
        counter.increment(bitmap)

        assertEquals(2, counter.count(bitmap))

        counter.decrement(bitmap)

        assertEquals(1, counter.count(bitmap))
    }

    @Test
    fun `valid bitmap is added to pool if count reaches zero`() {
        val key = Key("key")
        val bitmap = createBitmap()

        weakMemoryCache.set(key, bitmap, false, 0)
        counter.increment(bitmap)

        assertEquals(1, counter.count(bitmap))

        assertTrue(counter.decrement(bitmap))

        assertEquals(0, counter.count(bitmap))
        assertEquals(bitmap, pool.getDirtyOrNull(bitmap.width, bitmap.height, bitmap.config))

        // The bitmap should be removed from the weak memory cache.
        assertNull(weakMemoryCache.get(key))
    }

    @Test
    fun `invalid bitmap is added to weak memory cache if count reaches zero`() {
        val key = Key("key")
        val bitmap = createBitmap()

        weakMemoryCache.set(key, bitmap, false, 0)
        counter.increment(bitmap)
        counter.invalidate(bitmap)

        assertEquals(1, counter.count(bitmap))

        assertFalse(counter.decrement(bitmap))

        assertEquals(0, counter.count(bitmap))
        assertNull(pool.getDirtyOrNull(bitmap.width, bitmap.height, bitmap.config))

        // The bitmap should still be present in the weak memory cache.
        assertEquals(bitmap, weakMemoryCache.get(key)?.bitmap)
    }
}
