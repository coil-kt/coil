package coil.memory

import coil.bitmap.BitmapPool
import coil.bitmap.FakeBitmapPool
import coil.bitmap.RealBitmapReferenceCounter
import coil.util.DEFAULT_BITMAP_SIZE
import coil.util.allocationByteCountCompat
import coil.util.createBitmap
import coil.util.isValid
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class RealMemoryCacheTest {

    private lateinit var bitmapPool: BitmapPool
    private lateinit var weakCache: WeakMemoryCache
    private lateinit var counter: RealBitmapReferenceCounter
    private lateinit var strongCache: StrongMemoryCache
    private lateinit var cache: MemoryCache

    @Before
    fun before() {
        bitmapPool = FakeBitmapPool()
        weakCache = RealWeakMemoryCache(null)
        counter = RealBitmapReferenceCounter(weakCache, bitmapPool, null)
        strongCache = StrongMemoryCache(weakCache, counter, Int.MAX_VALUE, null)
        cache = RealMemoryCache(strongCache, weakCache, counter, bitmapPool)
    }

    @Test
    fun `can retrieve strong cached value`() {
        val key = MemoryCache.Key("strong")
        val bitmap = createBitmap()

        assertNull(cache[key])

        counter.setValid(bitmap, true)
        strongCache.set(key, bitmap, false)

        assertTrue(counter.isValid(bitmap))
        assertEquals(bitmap, cache[key])
        assertFalse(counter.isValid(bitmap))
    }

    @Test
    fun `can retrieve weak cached value`() {
        val key = MemoryCache.Key("weak")
        val bitmap = createBitmap()

        assertNull(cache[key])

        counter.setValid(bitmap, true)
        weakCache.set(key, bitmap, false, bitmap.allocationByteCountCompat)

        assertTrue(counter.isValid(bitmap))
        assertEquals(bitmap, cache[key])
        assertFalse(counter.isValid(bitmap))
    }

    @Test
    fun `remove removes from both caches`() {
        val key = MemoryCache.Key("key")
        val bitmap = createBitmap()

        assertNull(cache[key])

        counter.setValid(bitmap, true)
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
        assertNull(cache[MemoryCache.Key("a")])
        assertNull(cache[MemoryCache.Key("b")])
        assertNull(cache[MemoryCache.Key("c")])
        assertNull(cache[MemoryCache.Key("d")])
    }

    @Test
    fun `set can be retrieved with get`() {
        val key = MemoryCache.Key("a")
        val bitmap = createBitmap()
        cache[key] = bitmap

        assertEquals(bitmap, cache[key])
    }

    @Test
    fun `set replaces strong and weak values`() {
        val key = MemoryCache.Key("a")
        val expected = createBitmap()

        strongCache.set(key, createBitmap(), false)
        weakCache.set(key, createBitmap(), false, 100)
        cache[key] = expected

        assertFalse(counter.isValid(expected))
        assertEquals(expected, strongCache.get(key)?.bitmap)
        assertNull(weakCache.get(key))
    }

    @Test
    fun `setting the same bitmap multiple times can only be removed once`() {
        val key = MemoryCache.Key("a")
        val bitmap = createBitmap()

        weakCache.set(key, bitmap, false, 100)
        weakCache.set(key, bitmap, false, 100)

        assertTrue(weakCache.remove(bitmap))
        assertFalse(weakCache.remove(bitmap))
    }
}
