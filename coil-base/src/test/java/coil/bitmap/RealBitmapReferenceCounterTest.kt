package coil.bitmap

import android.graphics.Bitmap
import androidx.collection.arraySetOf
import androidx.collection.size
import coil.memory.MemoryCache.Key
import coil.memory.RealWeakMemoryCache
import coil.memory.WeakMemoryCache
import coil.util.DEFAULT_BITMAP_SIZE
import coil.util.clear
import coil.util.count
import coil.util.createBitmap
import coil.util.executeQueuedMainThreadTasks
import coil.util.identityHashCode
import coil.util.isValid
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class RealBitmapReferenceCounterTest {

    private lateinit var weakMemoryCache: WeakMemoryCache
    private lateinit var pool: BitmapPool
    private lateinit var counter: RealBitmapReferenceCounter

    @Before
    fun before() {
        weakMemoryCache = RealWeakMemoryCache(null)
        pool = BitmapPool(DEFAULT_BITMAP_SIZE)
        counter = RealBitmapReferenceCounter(weakMemoryCache, pool, null)
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
        counter.setValid(bitmap, true)
        counter.increment(bitmap)

        assertEquals(1, counter.count(bitmap))

        assertTrue(counter.decrement(bitmap))

        executeQueuedMainThreadTasks()

        assertEquals(0, counter.count(bitmap))
        assertSame(bitmap, pool.getDirtyOrNull(bitmap.width, bitmap.height, bitmap.config))

        // The bitmap should be removed from the weak memory cache.
        assertNull(weakMemoryCache.get(key))
    }

    @Test
    fun `invalid bitmap is added to weak memory cache if count reaches zero`() {
        val key = Key("key")
        val bitmap = createBitmap()

        weakMemoryCache.set(key, bitmap, false, 0)
        counter.increment(bitmap)
        counter.setValid(bitmap, false)

        assertEquals(1, counter.count(bitmap))

        assertFalse(counter.decrement(bitmap))

        assertEquals(0, counter.count(bitmap))
        assertNull(pool.getDirtyOrNull(bitmap.width, bitmap.height, bitmap.config))

        // The bitmap should still be present in the weak memory cache.
        assertEquals(bitmap, weakMemoryCache.get(key)?.bitmap)
    }

    @Test
    fun `invalid bitmaps cannot be made valid again`() {
        val bitmap = createBitmap()

        counter.setValid(bitmap, true)

        assertTrue(counter.isValid(bitmap))

        counter.setValid(bitmap, false)

        assertFalse(counter.isValid(bitmap))

        counter.setValid(bitmap, true)

        assertFalse(counter.isValid(bitmap))
    }

    @Test
    fun `invalid bitmaps are not removed from values`() {
        val bitmap = createBitmap()

        counter.setValid(bitmap, true)
        counter.increment(bitmap)
        counter.setValid(bitmap, false)
        counter.decrement(bitmap)

        assertFalse(counter.isValid(bitmap))
        assertSame(bitmap, counter.values[bitmap.identityHashCode]?.bitmap?.get())
    }

    @Test
    fun `cleanUp clears all collected values`() {
        val references = arraySetOf<Bitmap>()
        fun reference(value: Bitmap) = value.also { references.add(value) }

        val bitmap1 = reference(createBitmap())
        counter.increment(bitmap1)

        val bitmap2 = reference(createBitmap())
        counter.increment(bitmap2)

        val bitmap3 = reference(createBitmap())
        counter.increment(bitmap3)

        assertEquals(3, counter.values.size)

        counter.clear(bitmap1)
        counter.clear(bitmap3)
        counter.cleanUp()

        assertEquals(1, counter.values.size)
        assertSame(bitmap2, counter.values.valueAt(0)?.bitmap?.get())
    }
}
