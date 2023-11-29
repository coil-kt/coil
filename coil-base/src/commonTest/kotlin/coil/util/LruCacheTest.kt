package coil.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LruCacheTest {

    @Test
    fun putAndGet() {
        val cache = LruCache<String, Int>(maxSize = 3)
        cache.put("one", 1)
        cache.put("two", 2)
        cache.put("three", 3)

        assertEquals(1, cache["one"])
        assertEquals(2, cache["two"])
        assertEquals(3, cache["three"])
    }

    @Test
    fun putUpdatesValue() {
        val cache = LruCache<String, Int>(maxSize = 3)
        cache.put("one", 1)
        cache.put("one", 10)

        assertEquals(10, cache["one"])
    }

    @Test
    fun getNonexistentKey() {
        val cache = LruCache<String, Int>(maxSize = 3)

        assertNull(cache["nonexistent"])
    }

    @Test
    fun evictionOnExceedingMaxSize() {
        val cache = LruCache<String, Int>(maxSize = 2)
        cache.put("one", 1)
        cache.put("two", 2)
        cache.put("three", 3)

        assertNull(cache["one"])
        assertEquals(2, cache["two"])
        assertEquals(3, cache["three"])
    }

    @Test
    fun lruOrderMaintained() {
        val cache = LruCache<String, Int>(maxSize = 3)
        cache.put("one", 1)
        cache.put("two", 2)
        cache.put("three", 3)

        // Access "one" to make it the most recently accessed.
        cache["one"]

        cache.put("four", 4)

        assertNull(cache["two"])
        assertEquals(1, cache["one"])
        assertEquals(3, cache["three"])
        assertEquals(4, cache["four"])
    }

    @Test
    fun clearCache() {
        val cache = LruCache<String, Int>(maxSize = 3)
        cache.put("one", 1)
        cache.put("two", 2)

        cache.clear()

        assertNull(cache["one"])
        assertNull(cache["two"])
    }
}
