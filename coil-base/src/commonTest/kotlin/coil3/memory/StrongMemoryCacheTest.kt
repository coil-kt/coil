package coil3.memory

import coil3.memory.MemoryCache.Key
import coil3.util.DEFAULT_FAKE_IMAGE_SIZE
import coil3.util.FakeImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class StrongMemoryCacheTest {

    @Test
    fun canRetrieveCachedValue() {
        val weakCache = EmptyWeakMemoryCache()
        val strongCache = RealStrongMemoryCache(2L * DEFAULT_FAKE_IMAGE_SIZE, weakCache)

        val image = FakeImage()
        strongCache.set(Key("1"), image, emptyMap(), image.size)

        assertEquals(image, strongCache.get(Key("1"))?.image)
    }

    @Test
    fun leastRecentlyUsedValueIsEvicted() {
        val weakCache = RealWeakMemoryCache()
        val strongCache = RealStrongMemoryCache(2L * DEFAULT_FAKE_IMAGE_SIZE, weakCache)

        val first = FakeImage()
        strongCache.set(Key("1"), first, emptyMap(), first.size)

        val second = FakeImage()
        strongCache.set(Key("2"), second, emptyMap(), second.size)

        val third = FakeImage()
        strongCache.set(Key("3"), third, emptyMap(), third.size)

        assertNull(strongCache.get(Key("1")))
        assertNotNull(weakCache.get(Key("1")))
    }

    @Test
    fun valueCanBeRemoved() {
        val weakCache = RealWeakMemoryCache()
        val strongCache = RealStrongMemoryCache(2L * DEFAULT_FAKE_IMAGE_SIZE, weakCache)

        val image = FakeImage()
        strongCache.set(Key("1"), image, emptyMap(), image.size)
        strongCache.remove(Key("1"))

        assertNull(strongCache.get(Key("1")))
        assertNotNull(weakCache.get(Key("1")))
    }
}
