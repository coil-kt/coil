package coil.memory

import coil.bitmap.BitmapPool
import coil.bitmap.RealBitmapReferenceCounter
import coil.memory.MemoryCache.Key
import coil.util.DEFAULT_BITMAP_SIZE
import coil.util.createBitmap
import coil.util.executeQueuedMainThreadTasks
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
class StrongMemoryCacheTest {

    @Test
    fun `can retrieve cached value`() {
        val weakCache = EmptyWeakMemoryCache
        val pool = BitmapPool(Int.MAX_VALUE)
        val counter = RealBitmapReferenceCounter(weakCache, pool, null)
        val strongCache = StrongMemoryCache(weakCache, counter, (2 * DEFAULT_BITMAP_SIZE), null)

        val bitmap = createBitmap()
        strongCache.set(Key("1"), bitmap, false)

        assertEquals(bitmap, strongCache.get(Key("1"))?.bitmap)
    }

    @Test
    fun `least recently used value is evicted`() {
        val weakCache = EmptyWeakMemoryCache
        val pool = BitmapPool(Int.MAX_VALUE)
        val counter = RealBitmapReferenceCounter(weakCache, pool, null)
        val strongCache = StrongMemoryCache(weakCache, counter, (2 * DEFAULT_BITMAP_SIZE), null)

        val first = createBitmap()
        strongCache.set(Key("1"), first, false)

        val second = createBitmap()
        strongCache.set(Key("2"), second, false)

        val third = createBitmap()
        strongCache.set(Key("3"), third, false)

        assertNull(strongCache.get(Key("1")))
    }

    @Test
    fun `maxSize 0 disables memory cache`() {
        val weakCache = EmptyWeakMemoryCache
        val pool = BitmapPool(Int.MAX_VALUE)
        val counter = RealBitmapReferenceCounter(weakCache, pool, null)
        val strongCache = StrongMemoryCache(weakCache, counter, 0, null)

        val bitmap = createBitmap()
        strongCache.set(Key("1"), bitmap, false)

        assertNull(strongCache.get(Key("1")))
    }

    @Test
    fun `value is removed after invalidate is called`() {
        val weakCache = RealWeakMemoryCache(null)
        val pool = BitmapPool(Int.MAX_VALUE)
        val counter = RealBitmapReferenceCounter(weakCache, pool, null)
        val strongCache = StrongMemoryCache(weakCache, counter, (2 * DEFAULT_BITMAP_SIZE), null)

        val bitmap = createBitmap()
        strongCache.set(Key("1"), bitmap, false)
        strongCache.remove(Key("1"))

        assertNull(strongCache.get(Key("1")))
    }

    @Test
    fun `valid evicted item is added to bitmap pool`() {
        val weakCache = RealWeakMemoryCache(null)
        val pool = BitmapPool(Int.MAX_VALUE)
        val counter = RealBitmapReferenceCounter(weakCache, pool, null)
        val strongCache = StrongMemoryCache(weakCache, counter, DEFAULT_BITMAP_SIZE, null)

        val first = createBitmap()
        counter.setValid(first, true)
        strongCache.set(Key("1"), first, false)

        assertNotNull(strongCache.get(Key("1")))

        val second = createBitmap()
        counter.setValid(second, true)
        strongCache.set(Key("2"), second, false)

        executeQueuedMainThreadTasks()

        assertNull(strongCache.get(Key("1")))
        assertNull(weakCache.get(Key("1")))
        assertSame(first, pool.getDirtyOrNull(first.width, first.height, first.config))
    }

    @Test
    fun `invalid evicted item is added to weak memory cache`() {
        val weakCache = RealWeakMemoryCache(null)
        val pool = BitmapPool(Int.MAX_VALUE)
        val counter = RealBitmapReferenceCounter(weakCache, pool, null)
        val strongCache = StrongMemoryCache(weakCache, counter, DEFAULT_BITMAP_SIZE, null)

        val first = createBitmap()
        strongCache.set(Key("key"), first, false)

        assertNotNull(strongCache.get(Key("key")))

        // Invalidate the first bitmap.
        counter.setValid(first, false)

        // Overwrite the value in the memory cache.
        val second = createBitmap()
        strongCache.set(Key("key"), second, false)

        assertEquals(second, strongCache.get(Key("key"))?.bitmap)
        assertEquals(first, weakCache.get(Key("key"))?.bitmap)
        assertNull(pool.getDirtyOrNull(first.width, first.height, first.config))
    }
}
