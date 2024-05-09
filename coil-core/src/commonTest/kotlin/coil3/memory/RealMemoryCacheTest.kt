package coil3.memory

import coil3.memory.MemoryCache.Key
import coil3.memory.MemoryCache.Value
import coil3.test.utils.FakeImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RealMemoryCacheTest {

    private val weakCache = RealWeakMemoryCache()
    private val strongCache = RealStrongMemoryCache(Long.MAX_VALUE, weakCache)
    private val cache = RealMemoryCache(strongCache, weakCache)

    @Test
    fun canRetrieveStrongCachedValue() {
        val key = Key("strong")
        val image = FakeImage()

        assertNull(cache[key])

        strongCache.set(key, image, emptyMap(), image.size)

        assertEquals(image, cache[key]?.image)
    }

    @Test
    fun canRetrieveWeakCachedValue() {
        val key = Key("weak")
        val image = FakeImage()

        assertNull(cache[key])

        weakCache.set(key, image, emptyMap(), image.size)

        assertEquals(image, cache[key]?.image)
    }

    @Test
    fun removeRemovesFromBothCaches() {
        val key = Key("key")
        val image = FakeImage()

        assertNull(cache[key])

        strongCache.set(key, image, emptyMap(), image.size)
        weakCache.set(key, image, emptyMap(), image.size)

        assertTrue(cache.remove(key))
        assertNull(strongCache.get(key))
        assertNull(weakCache.get(key))
    }

    @Test
    fun clearClearsAllValues() {
        assertEquals(0, cache.size)

        strongCache.set(Key("a"), FakeImage(), emptyMap(), 100)
        strongCache.set(Key("b"), FakeImage(), emptyMap(), 100)
        weakCache.set(Key("c"), FakeImage(), emptyMap(), 100)
        weakCache.set(Key("d"), FakeImage(), emptyMap(), 100)

        assertEquals(2L * 100, cache.size)

        cache.clear()

        assertEquals(0, cache.size)
        assertNull(cache[Key("a")])
        assertNull(cache[Key("b")])
        assertNull(cache[Key("c")])
        assertNull(cache[Key("d")])
    }

    @Test
    fun setCanBeRetrievedWithGet() {
        val key = Key("a")
        val image = FakeImage()
        cache[key] = Value(image)

        assertEquals(image, cache[key]?.image)
    }

    @Test
    fun settingTheSameImageMultipleTimesCanOnlyBeRemovedOnce() {
        val key = Key("a")
        val image = FakeImage()

        weakCache.set(key, image, emptyMap(), 100)
        weakCache.set(key, image, emptyMap(), 100)

        assertTrue(weakCache.remove(key))
        assertFalse(weakCache.remove(key))
    }
}
