package coil.memory

import coil.annotation.ExperimentalCoilApi
import coil.bitmappool.FakeBitmapPool
import coil.util.allocationByteCountCompat
import coil.util.createBitmap
import coil.util.isInvalid
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        strongCache.set(key, bitmap, false)

        assertFalse(counter.isInvalid(bitmap))
        assertEquals(bitmap, cache.get(key))
        assertTrue(counter.isInvalid(bitmap))
    }

    @Test
    fun `can retrieve weak cached value`() {
        val key = MemoryCache.Key("weak")
        val bitmap = createBitmap()
        weakCache.set(key, bitmap, false, bitmap.allocationByteCountCompat)

        assertFalse(counter.isInvalid(bitmap))
        assertEquals(bitmap, cache.get(key))
        assertTrue(counter.isInvalid(bitmap))
    }
}
