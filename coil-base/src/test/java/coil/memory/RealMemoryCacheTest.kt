package coil.memory

import coil.annotation.ExperimentalCoilApi
import coil.bitmappool.FakeBitmapPool
import coil.util.DEFAULT_BITMAP_SIZE
import coil.util.allocationByteCountCompat
import coil.util.createBitmap
import coil.util.isInvalid
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoilApi::class)
class RealMemoryCacheTest {

    private lateinit var weakCache: WeakMemoryCache
    private lateinit var counter: BitmapReferenceCounter
    private lateinit var strongCache: StrongMemoryCache
    private lateinit var cache: MemoryCache

    @Before
    fun before() {
        weakCache = RealWeakMemoryCache(null)
        counter = BitmapReferenceCounter(weakCache, FakeBitmapPool(), null)
        strongCache = StrongMemoryCache(weakCache, counter, Int.MAX_VALUE, null)
        cache = RealMemoryCache(strongCache, weakCache, counter)
    }

    @Test
    fun `can retrieve strong cached value`() {
        val key = MemoryCache.Key("strong")
        val bitmap = createBitmap()

        assertNull(cache.get(key))

        strongCache.set(key, bitmap, false)

        assertFalse(counter.isInvalid(bitmap))
        assertEquals(bitmap, cache.get(key))
        assertTrue(counter.isInvalid(bitmap))
    }

    @Test
    fun `can retrieve weak cached value`() {
        val key = MemoryCache.Key("weak")
        val bitmap = createBitmap()

        assertNull(cache.get(key))

        weakCache.set(key, bitmap, false, bitmap.allocationByteCountCompat)

        assertFalse(counter.isInvalid(bitmap))
        assertEquals(bitmap, cache.get(key))
        assertTrue(counter.isInvalid(bitmap))
    }

    @Test
    fun `remove removes from both caches`() {
        val key = MemoryCache.Key("key")
        val bitmap = createBitmap()

        assertNull(cache.get(key))

        strongCache.set(key, bitmap, false)
        weakCache.set(key, bitmap, false, bitmap.allocationByteCountCompat)

        assertTrue(cache.remove(key))
        assertNull(strongCache.get(key))
        assertNull(weakCache.get(key))
    }

    @Test
    fun `clear clears all values`() {
        assertEquals(0, cache.size)

        strongCache.set(MemoryCache.Key("a"), createBitmap(), false)
        strongCache.set(MemoryCache.Key("b"), createBitmap(), false)
        weakCache.set(MemoryCache.Key("c"), createBitmap(), false, 100)
        weakCache.set(MemoryCache.Key("d"), createBitmap(), false, 100)

        assertEquals(2 * DEFAULT_BITMAP_SIZE, cache.size)

        cache.clear()

        assertEquals(0, cache.size)
        assertNull(cache.get(MemoryCache.Key("a")))
        assertNull(cache.get(MemoryCache.Key("b")))
        assertNull(cache.get(MemoryCache.Key("c")))
        assertNull(cache.get(MemoryCache.Key("d")))
    }
}
