package coil3.memory

import coil3.Image
import coil3.memory.MemoryCache.Key
import coil3.memory.MemoryCache.Value
import coil3.test.utils.DEFAULT_FAKE_IMAGE_SIZE
import coil3.test.utils.FakeImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class StrongMemoryCacheTest {

    @Test
    fun canRetrieveCachedValue() {
        val weakCache = FakeWeakMemoryCache()
        val strongCache = RealStrongMemoryCache(2 * DEFAULT_FAKE_IMAGE_SIZE, weakCache)

        val image = FakeImage()
        strongCache.set(Key("1"), image, emptyMap(), image.size)

        assertEquals(image, strongCache.get(Key("1"))?.image)
    }

    @Test
    fun leastRecentlyUsedValueIsEvicted() {
        val weakCache = FakeWeakMemoryCache()
        val strongCache = RealStrongMemoryCache(2 * DEFAULT_FAKE_IMAGE_SIZE, weakCache)

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
        val weakCache = FakeWeakMemoryCache()
        val strongCache = RealStrongMemoryCache(2 * DEFAULT_FAKE_IMAGE_SIZE, weakCache)

        val image = FakeImage()
        strongCache.set(Key("1"), image, emptyMap(), image.size)
        strongCache.remove(Key("1"))

        assertNull(strongCache.get(Key("1")))
        assertNotNull(weakCache.get(Key("1")))
    }
}

private class FakeWeakMemoryCache : WeakMemoryCache {
    private val map = mutableMapOf<Key, Value>()

    override val keys: Set<Key>
        get() = map.keys

    override fun get(key: Key): Value? {
        return map[key]
    }

    override fun set(key: Key, image: Image, extras: Map<String, Any>, size: Long) {
        map[key] = Value(image, extras)
    }

    override fun remove(key: Key): Boolean {
        return map.remove(key) != null
    }

    override fun clear() {
        map.clear()
    }
}
