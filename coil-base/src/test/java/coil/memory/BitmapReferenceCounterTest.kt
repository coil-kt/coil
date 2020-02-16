package coil.memory

import coil.bitmappool.BitmapPool
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
class BitmapReferenceCounterTest {

    private lateinit var weakMemoryCache: WeakMemoryCache
    private lateinit var pool: BitmapPool
    private lateinit var counter: BitmapReferenceCounter

    @Before
    fun before() {
        weakMemoryCache = WeakMemoryCache()
        pool = RealBitmapPool(DEFAULT_BITMAP_SIZE)
        counter = BitmapReferenceCounter(weakMemoryCache, pool)
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
    fun `bitmap is added to pool if count reaches zero`() {
        val bitmap = createBitmap()
        counter.increment(bitmap)

        assertEquals(1, counter.count(bitmap))

        counter.decrement(bitmap)

        assertEquals(0, counter.count(bitmap))
        assertEquals(bitmap, pool.getDirtyOrNull(bitmap.width, bitmap.height, bitmap.config))
    }

    @Test
    fun `invalid bitmap is not added to pool if count reaches zero`() {
        val bitmap = createBitmap()
        counter.increment(bitmap)
        counter.invalidate(bitmap)

        assertEquals(1, counter.count(bitmap))

        counter.decrement(bitmap)

        assertEquals(0, counter.count(bitmap))
        assertNull(pool.getDirtyOrNull(bitmap.width, bitmap.height, bitmap.config))
    }
}
