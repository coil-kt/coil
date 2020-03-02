package coil.memory

import coil.bitmappool.BitmapPool
import coil.util.DEFAULT_BITMAP_SIZE
import coil.util.createBitmap
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class MemoryCacheTest {

    @Test
    fun `can retrieve cached value`() {
        val weakMemoryCache = EmptyWeakMemoryCache
        val pool = BitmapPool(0)
        val counter = BitmapReferenceCounter(weakMemoryCache, pool)
        val cache = MemoryCache(weakMemoryCache, counter, (2 * DEFAULT_BITMAP_SIZE).toInt())

        val bitmap = createBitmap()
        cache.set("1", bitmap, false)

        assertEquals(bitmap, cache.get("1")?.bitmap)
    }

    @Test
    fun `least recently used value is evicted`() {
        val weakMemoryCache = EmptyWeakMemoryCache
        val pool = BitmapPool(0)
        val counter = BitmapReferenceCounter(weakMemoryCache, pool)
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
        val weakMemoryCache = EmptyWeakMemoryCache
        val pool = BitmapPool(0)
        val counter = BitmapReferenceCounter(weakMemoryCache, pool)
        val cache = MemoryCache(weakMemoryCache, counter, 0)

        val bitmap = createBitmap()
        cache.set("1", bitmap, false)

        assertNull(cache.get("1"))
    }

    @Test
    fun `evicted item is added to bitmap pool`() {
        val weakMemoryCache = RealWeakMemoryCache()
        val pool = BitmapPool(Long.MAX_VALUE)
        val counter = BitmapReferenceCounter(weakMemoryCache, pool)
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
        val weakMemoryCache = RealWeakMemoryCache()
        val pool = BitmapPool(Long.MAX_VALUE)
        val counter = BitmapReferenceCounter(weakMemoryCache, pool)
        val cache = MemoryCache(weakMemoryCache, counter, DEFAULT_BITMAP_SIZE.toInt())

        val first = createBitmap()
        cache.set("key", first, false)

        assertNotNull(cache.get("key"))

        // Invalidate the first bitmap.
        counter.invalidate(first)

        // Overwrite the value in the memory cache.
        val second = createBitmap()
        cache.set("key", second, false)

        assertEquals(second, cache.get("key")?.bitmap)
        assertEquals(first, weakMemoryCache.get("key")?.bitmap)
        assertNull(pool.getDirtyOrNull(first.width, first.height, first.config))
    }
}
