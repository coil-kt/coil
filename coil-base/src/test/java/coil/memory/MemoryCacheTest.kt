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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class MemoryCacheTest {

    private lateinit var weakMemoryCache: RealWeakMemoryCache
    private lateinit var pool: BitmapPool
    private lateinit var counter: BitmapReferenceCounter

    @Before
    fun before() {
        weakMemoryCache = RealWeakMemoryCache()
        pool = RealBitmapPool(Long.MAX_VALUE)
        counter = BitmapReferenceCounter(weakMemoryCache, pool)
    }

    @Test
    fun `can retrieve cached value`() {
        val cache = MemoryCache(weakMemoryCache, counter, (2 * DEFAULT_BITMAP_SIZE).toInt())

        val bitmap = createBitmap()
        cache.set("1", bitmap, false)

        assertEquals(bitmap, cache.get("1")?.bitmap)
    }

    @Test
    fun `least recently used value is evicted`() {
        val cache = MemoryCache(weakMemoryCache, counter, (2 * DEFAULT_BITMAP_SIZE).toInt())

        val first = createBitmap()
        cache.set("1", first, false)

        val second = createBitmap()
        cache.set("2", second, false)

        val third = createBitmap()
        cache.set("3", third, false)

        assertNull(cache.get("1"))
    }

    @Test
    fun `maxSize 0 disables memory cache`() {
        val cache = MemoryCache(weakMemoryCache, counter, 0)

        val bitmap = createBitmap()
        cache.set("1", bitmap, false)

        assertNull(cache.get("1"))
    }

    @Test
    fun `evicted item is added to bitmap pool`() {
        val cache = MemoryCache(weakMemoryCache, counter, DEFAULT_BITMAP_SIZE.toInt())

        val first = createBitmap()
        cache.set("1", first, false)

        assertNotNull(cache.get("1"))

        val second = createBitmap()
        cache.set("2", second, false)

        assertNull(cache.get("1"))
        assertNull(weakMemoryCache.get("1"))
        assertEquals(first, pool.getDirtyOrNull(first.width, first.height, first.config))
    }

    @Test
    fun `invalid evicted item is added to weak memory cache`() {
        val cache = MemoryCache(weakMemoryCache, counter, DEFAULT_BITMAP_SIZE.toInt())

        val first = createBitmap()
        cache.set("key", first, false)

        assertNotNull(cache.get("key"))

        // Overwrite the value in the memory cache.
        val second = createBitmap()
        cache.set("key", second, false)

        assertEquals(second, cache.get("key")?.bitmap)
        assertEquals(first, weakMemoryCache.get("key")?.bitmap)
        assertNull(pool.getDirtyOrNull(first.width, first.height, first.config))
    }
}
