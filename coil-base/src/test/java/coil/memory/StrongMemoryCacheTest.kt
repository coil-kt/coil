package coil.memory

import coil.annotation.ExperimentalCoilApi
import coil.bitmappool.BitmapPool
import coil.memory.MemoryCache.Key
import coil.util.DEFAULT_BITMAP_SIZE
import coil.util.createBitmap
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoilApi::class)
class StrongMemoryCacheTest {

    @Test
    fun `can retrieve cached value`() {
        val weakMemoryCache = EmptyWeakMemoryCache
        val pool = BitmapPool(Int.MAX_VALUE)
        val counter = BitmapReferenceCounter(weakMemoryCache, pool, null)
        val cache = StrongMemoryCache(weakMemoryCache, counter, (2 * DEFAULT_BITMAP_SIZE), null)

        val bitmap = createBitmap()
        cache.set(Key("1"), bitmap, false)

        assertEquals(bitmap, cache.get(Key("1"))?.bitmap)
    }

    @Test
    fun `least recently used value is evicted`() {
        val weakMemoryCache = EmptyWeakMemoryCache
        val pool = BitmapPool(Int.MAX_VALUE)
        val counter = BitmapReferenceCounter(weakMemoryCache, pool, null)
        val cache = StrongMemoryCache(weakMemoryCache, counter, (2 * DEFAULT_BITMAP_SIZE), null)

        val first = createBitmap()
        cache.set(Key("1"), first, false)

        val second = createBitmap()
        cache.set(Key("2"), second, false)

        val third = createBitmap()
        cache.set(Key("3"), third, false)

        assertNull(cache.get(Key("1")))
    }

    @Test
    fun `maxSize 0 disables memory cache`() {
        val weakMemoryCache = EmptyWeakMemoryCache
        val pool = BitmapPool(Int.MAX_VALUE)
        val counter = BitmapReferenceCounter(weakMemoryCache, pool, null)
        val cache = StrongMemoryCache(weakMemoryCache, counter, 0, null)

        val bitmap = createBitmap()
        cache.set(Key("1"), bitmap, false)

        assertNull(cache.get(Key("1")))
    }

    @Test
    fun `value is removed after invalidate is called`() {
        val weakMemoryCache = RealWeakMemoryCache(null)
        val pool = BitmapPool(Int.MAX_VALUE)
        val counter = BitmapReferenceCounter(weakMemoryCache, pool, null)
        val cache = StrongMemoryCache(weakMemoryCache, counter, (2 * DEFAULT_BITMAP_SIZE), null)

        val bitmap = createBitmap()
        cache.set(Key("1"), bitmap, false)
        cache.remove(Key("1"))

        assertNull(cache.get(Key("1")))
    }

    @Test
    fun `valid evicted item is added to bitmap pool`() {
        val weakMemoryCache = RealWeakMemoryCache(null)
        val pool = BitmapPool(Int.MAX_VALUE)
        val counter = BitmapReferenceCounter(weakMemoryCache, pool, null)
        val cache = StrongMemoryCache(weakMemoryCache, counter, DEFAULT_BITMAP_SIZE, null)

        val first = createBitmap()
        cache.set(Key("1"), first, false)

        assertNotNull(cache.get(Key("1")))

        val second = createBitmap()
        cache.set(Key("2"), second, false)

        assertNull(cache.get(Key("1")))
        assertNull(weakMemoryCache.get(Key("1")))
        assertEquals(first, pool.getDirtyOrNull(first.width, first.height, first.config))
    }

    @Test
    fun `invalid evicted item is added to weak memory cache`() {
        val weakMemoryCache = RealWeakMemoryCache(null)
        val pool = BitmapPool(Int.MAX_VALUE)
        val counter = BitmapReferenceCounter(weakMemoryCache, pool, null)
        val cache = StrongMemoryCache(weakMemoryCache, counter, DEFAULT_BITMAP_SIZE, null)

        val first = createBitmap()
        cache.set(Key("key"), first, false)

        assertNotNull(cache.get(Key("key")))

        // Invalidate the first bitmap.
        counter.invalidate(first)

        // Overwrite the value in the memory cache.
        val second = createBitmap()
        cache.set(Key("key"), second, false)

        assertEquals(second, cache.get(Key("key"))?.bitmap)
        assertEquals(first, weakMemoryCache.get(Key("key"))?.bitmap)
        assertNull(pool.getDirtyOrNull(first.width, first.height, first.config))
    }
}
